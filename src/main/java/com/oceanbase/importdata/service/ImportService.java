package com.oceanbase.importdata.service;

import com.oceanbase.importdata.entity.DataSourceConfig;
import com.oceanbase.importdata.entity.ImportBatch;
import com.oceanbase.importdata.entity.ImportTask;
import com.oceanbase.importdata.repository.BatchRepository;
import com.oceanbase.importdata.repository.TaskRepository;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final TaskRepository taskRepository;
    private final BatchRepository batchRepository;
    private final DataSourceManagerService dataSourceManagerService;
    private final Map<String, AtomicBoolean> taskStopFlags = new ConcurrentHashMap<>();
    private final List<String> runningTasks = new CopyOnWriteArrayList<>();

    public ImportService(TaskRepository taskRepository, BatchRepository batchRepository,
                        DataSourceManagerService dataSourceManagerService) {
        this.taskRepository = taskRepository;
        this.batchRepository = batchRepository;
        this.dataSourceManagerService = dataSourceManagerService;
    }

    public String createImportTask(ImportTask task) {
        String taskId = UUID.randomUUID().toString();
        task.setTaskId(taskId);
        task.setStatus("WAITING");
        task.setCreateTime(LocalDateTime.now());
        taskRepository.insert(task);
        return taskId;
    }

    @Async
    public void executeImport(String taskId) {
        ImportTask task = taskRepository.findById(taskId);
        if (task == null) {
            log.error("Task not found: {}", taskId);
            return;
        }

        AtomicBoolean stopFlag = new AtomicBoolean(false);
        taskStopFlags.put(taskId, stopFlag);
        runningTasks.add(taskId);

        long overallStart = System.currentTimeMillis();
        try {
            taskRepository.updateStartTime(taskId, LocalDateTime.now());

            Path filePath = Path.of(task.getFilePath());
            if (!Files.exists(filePath)) {
                throw new IOException("File not found: " + task.getFilePath());
            }

            // 读取文件总行数（快速统计）
            long countStart = System.currentTimeMillis();
            int totalLines = 0;
            try (BufferedReader reader = Files.newBufferedReader(filePath, Charset.forName(task.getFileEncoding()))) {
                while (reader.readLine() != null) {
                    totalLines++;
                }
            }
            log.info("Counted {} lines in {}ms", totalLines, System.currentTimeMillis() - countStart);

            task.setTotalRows((long) totalLines);
            taskRepository.update(task);

            // 创建一个批次用于UI显示（实际不分批次）
            ImportBatch singleBatch = new ImportBatch();
            singleBatch.setBatchId(UUID.randomUUID().toString());
            singleBatch.setTaskId(taskId);
            singleBatch.setBatchNumber(1);
            singleBatch.setStatus("PENDING");
            singleBatch.setStartPosition(1L);
            singleBatch.setEndPosition((long) totalLines);
            singleBatch.setRowsCount(totalLines);
            batchRepository.insert(singleBatch);

            if (stopFlag.get()) {
                log.info("Task stopped by user: {}", taskId);
                taskRepository.updateEndTime(taskId, LocalDateTime.now(), "STOPPED");
            } else {
                batchRepository.updateStartTime(singleBatch.getBatchId(), LocalDateTime.now());

                int imported = processEntireFile(filePath, Charset.forName(task.getFileEncoding()), task);

                taskRepository.updateProgress(taskId, (long) imported, "IMPORTING");

                batchRepository.updateEndTime(singleBatch.getBatchId(), LocalDateTime.now(), "COMPLETED");

                taskRepository.updateEndTime(taskId, LocalDateTime.now(), "COMPLETED");
                
                long duration = System.currentTimeMillis() - overallStart;
                log.info("✅ COMPLETED! Total time: {}ms, Rows: {}, Speed: {} rows/s", 
                        duration, imported, Math.round(imported * 1000.0 / duration));
            }

        } catch (Exception e) {
            log.error("Import task failed: {}", taskId, e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 4000) {
                errorMsg = errorMsg.substring(0, 4000);
            }
            taskRepository.updateStatus(taskId, "FAILED", errorMsg);
        } finally {
            taskStopFlags.remove(taskId);
            runningTasks.remove(taskId);
        }
    }

    private int processEntireFile(Path filePath, Charset charset, ImportTask task) throws IOException, SQLException {
        boolean skipHeader = task.getSkipHeader() != null && task.getSkipHeader() == 1;
        int totalImported = 0;
        int batchCount = 0;

        // 动态批次大小 - 为 OceanBase 优化
        int configuredBatchSize = task.getBatchSize() != null && task.getBatchSize() > 0 ? task.getBatchSize() : 5000;
        // 限制最大批次大小为 200000，避免内存问题
        final int JDBC_BATCH_SIZE = Math.min(configuredBatchSize, 200000);
        log.info("Processing with batch size: {}", JDBC_BATCH_SIZE);

        long start = System.currentTimeMillis();
        Connection conn = null;
        PreparedStatement pstmt = null;
        List<String[]> batchData = new ArrayList<>(JDBC_BATCH_SIZE);
        CsvParser parser = null;
        
        try {
            // 获取数据源
            if (task.getDataSourceConfigId() != null && !task.getDataSourceConfigId().isEmpty()) {
                DataSource targetDataSource = dataSourceManagerService.getDataSource(task.getDataSourceConfigId());
                conn = targetDataSource.getConnection();
            } else {
                // 没有配置，使用默认数据源？
                // 这里我们需要从 DataSourceManagerService 获取默认配置
                DataSourceConfig defaultConfig = dataSourceManagerService.getDefaultConfig();
                if (defaultConfig != null) {
                    DataSource targetDataSource = dataSourceManagerService.getDataSource(defaultConfig.getConfigId());
                    conn = targetDataSource.getConnection();
                } else {
                    throw new RuntimeException("No data source configured");
                }
            }
            conn.setAutoCommit(false);
            try {
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            } catch (SQLException e) {
                log.warn("Could not set transaction isolation", e);
            }

            // 高性能 CSV 解析配置
            CsvParserSettings parserSettings = new CsvParserSettings();
            parserSettings.getFormat().setDelimiter(task.getDelimiter().charAt(0));
            parserSettings.setLineSeparatorDetectionEnabled(true);
            parserSettings.setReadInputOnSeparateThread(false);
            parserSettings.setMaxCharsPerColumn(100000);
            
            // 读取文件
            Reader reader = Files.newBufferedReader(filePath, charset);
            parser = new CsvParser(parserSettings);
            parser.beginParsing(reader);

            // 读取第一行
            String[] firstRow = parser.parseNext();
            if (firstRow == null || firstRow.length == 0) {
                throw new IOException("File is empty!");
            }
            
            String[] columnNames;
            if (skipHeader) {
                // 用户选择跳过表头，第一行就是表头，不插入
                columnNames = firstRow;
                log.info("Skip header - Columns count: {}", columnNames.length);
            } else {
                // 用户不跳过表头，需要自动生成列名，第一行作为数据插入
                columnNames = new String[firstRow.length];
                for (int i = 0; i < firstRow.length; i++) {
                    columnNames[i] = "COLUMN_" + (i + 1);
                }
                log.info("No header - Columns count: {}, first row as data", columnNames.length);
                batchData.add(firstRow);
            }
            
            log.info("Columns count: {}", columnNames.length);

            // 创建 INSERT SQL
            pstmt = createInsertStatement(conn, task.getTargetTable(), columnNames);

            // 处理数据
            String[] row;
            while ((row = parser.parseNext()) != null) {
                batchData.add(row);
                
                // 达到批次大小，执行批量插入
                if (batchData.size() >= JDBC_BATCH_SIZE) {
                    int count = executeInsertBatch(pstmt, batchData, columnNames);
                    totalImported += count;
                    batchCount++;
                    
                    // 每次提交并更新进度
                    conn.commit();
                    taskRepository.updateProgress(task.getTaskId(), (long) totalImported, "IMPORTING");
                    
                    // 定期记录日志
                    if (batchCount % 2 == 0) {
                        log.info("Processed {} batches, {} rows", batchCount, totalImported);
                    }
                    
                    batchData.clear();
                }
            }

            // 处理剩余批次
            if (!batchData.isEmpty()) {
                int count = executeInsertBatch(pstmt, batchData, columnNames);
                totalImported += count;
                conn.commit();
            }

            long duration = System.currentTimeMillis() - start;
            log.info("⏱️  Data processing: {}ms, Rows: {}, Speed: {} rows/s", 
                    duration, totalImported, Math.round(totalImported * 1000.0 / duration));

            return totalImported;

        } catch (Exception e) {
            log.error("Processing failed", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    log.error("Rollback failed", ex);
                }
            }
            throw e;
        } finally {
            if (parser != null) {
                parser.stopParsing();
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    log.warn("Close statement failed", e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("Close connection failed", e);
                }
            }
        }
    }

    private PreparedStatement createInsertStatement(Connection conn, String tableName, String[] columnNames) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) sql.append(",");
            sql.append(columnNames[i]);
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");
        return conn.prepareStatement(sql.toString());
    }

    private int executeInsertBatch(PreparedStatement pstmt, List<String[]> batchData, String[] columnNames) throws SQLException {
        int count = 0;
        for (String[] row : batchData) {
            // 设置参数
            int colCount = Math.min(row.length, columnNames.length);
            for (int i = 0; i < colCount; i++) {
                pstmt.setString(i + 1, row[i]);
            }
            // 剩余的列设置为null（防止列数不匹配）
            for (int i = colCount; i < columnNames.length; i++) {
                pstmt.setString(i + 1, null);
            }
            pstmt.addBatch();
            count++;
        }
        pstmt.executeBatch();
        return count;
    }

    public void stopImport(String taskId) {
        AtomicBoolean stopFlag = taskStopFlags.get(taskId);
        if (stopFlag != null) {
            stopFlag.set(true);
            ImportTask task = taskRepository.findById(taskId);
            if (task != null && "IMPORTING".equals(task.getStatus())) {
                taskRepository.updateStatus(taskId, "STOPPING", null);
            }
        }
    }

    public List<String> getRunningTasks() {
        return new ArrayList<>(runningTasks);
    }

    public ImportTask getTask(String taskId) {
        return taskRepository.findById(taskId);
    }

    public List<ImportTask> getTasks(int page, int size, String status) {
        return taskRepository.findAll(page, size, status);
    }

    public int getTaskCount(String status) {
        return taskRepository.countAll(status);
    }

    public List<ImportBatch> getTaskBatches(String taskId) {
        return batchRepository.findByTaskId(taskId);
    }

    public void cancelTask(String taskId) {
        stopImport(taskId);
    }

    public void deleteTask(String taskId) {
        batchRepository.deleteByTaskId(taskId);
        taskRepository.deleteById(taskId);
    }

    public List<ImportBatch> getFailedBatches(String taskId) {
        return batchRepository.findFailedByTaskId(taskId);
    }

    public Map<String, Object> getTaskProgress(String taskId) {
        ImportTask task = taskRepository.findById(taskId);
        if (task == null) {
            return null;
        }
        Map<String, Object> progress = new java.util.HashMap<>();
        progress.put("taskId", taskId);
        progress.put("status", task.getStatus());
        progress.put("importedRows", task.getImportedRows());
        progress.put("totalRows", task.getTotalRows());
        progress.put("progressPercentage", task.getProgressPercentage());
        return progress;
    }

    public boolean retryBatch(String batchId) {
        return false;
    }

    public Map<String, Integer> getTaskStats() {
        return taskRepository.getTaskStats();
    }
}
