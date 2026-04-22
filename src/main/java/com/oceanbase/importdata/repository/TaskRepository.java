package com.oceanbase.importdata.repository;

import com.oceanbase.importdata.entity.ImportTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class TaskRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<ImportTask> TASK_ROW_MAPPER = new RowMapper<ImportTask>() {
        @Override
        public ImportTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            ImportTask task = new ImportTask();
            task.setTaskId(rs.getString("task_id"));
            task.setFileName(rs.getString("file_name"));
            task.setTargetTable(rs.getString("target_table"));
            task.setDataSourceConfigId(rs.getString("data_source_config_id"));
            task.setTotalRows(rs.getLong("total_rows"));
            task.setImportedRows(rs.getLong("imported_rows"));
            task.setStatus(rs.getString("status"));
            task.setFileEncoding(rs.getString("file_encoding"));
            task.setDelimiter(rs.getString("delimiter"));
            task.setSkipHeader(rs.getInt("skip_header"));
            task.setCreateTime(toLocalDateTime(rs, "create_time"));
            task.setStartTime(toLocalDateTime(rs, "start_time"));
            task.setEndTime(toLocalDateTime(rs, "end_time"));
            task.setErrorMessage(rs.getString("error_message"));
            task.setBatchSize(rs.getInt("batch_size"));
            task.setThreadCount(rs.getInt("thread_count"));
            task.setFilePath(rs.getString("file_path"));
            return task;
        }

        private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
            java.sql.Timestamp ts = rs.getTimestamp(column);
            return ts != null ? ts.toLocalDateTime() : null;
        }
    };

    public String insert(ImportTask task) {
        if (task.getTaskId() == null) {
            task.setTaskId(UUID.randomUUID().toString());
        }
        String sql = "INSERT INTO import_tasks (task_id, file_name, target_table, data_source_config_id, total_rows, imported_rows, " +
                "status, file_encoding, delimiter, skip_header, create_time, batch_size, thread_count, file_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, task.getTaskId(), task.getFileName(), task.getTargetTable(),
                task.getDataSourceConfigId(), task.getTotalRows(), task.getImportedRows(), task.getStatus(), 
                task.getFileEncoding(), task.getDelimiter(), task.getSkipHeader(), task.getCreateTime(), 
                task.getBatchSize(), task.getThreadCount(), task.getFilePath());
        return task.getTaskId();
    }

    public int update(ImportTask task) {
        String sql = "UPDATE import_tasks SET total_rows = ?, imported_rows = ?, status = ?, " +
                "start_time = ?, end_time = ?, error_message = ?, file_path = ? WHERE task_id = ?";
        return jdbcTemplate.update(sql, task.getTotalRows(), task.getImportedRows(), task.getStatus(),
                task.getStartTime(), task.getEndTime(), task.getErrorMessage(), task.getFilePath(), task.getTaskId());
    }

    public int updateProgress(String taskId, Long importedRows, String status) {
        String sql = "UPDATE import_tasks SET imported_rows = ?, status = ? WHERE task_id = ?";
        return jdbcTemplate.update(sql, importedRows, status, taskId);
    }

    public int updateStatus(String taskId, String status, String errorMessage) {
        String sql = "UPDATE import_tasks SET status = ?, error_message = ? WHERE task_id = ?";
        return jdbcTemplate.update(sql, status, errorMessage, taskId);
    }

    public int updateStartTime(String taskId, LocalDateTime startTime) {
        String sql = "UPDATE import_tasks SET start_time = ?, status = 'IMPORTING' WHERE task_id = ?";
        return jdbcTemplate.update(sql, startTime, taskId);
    }

    public int updateEndTime(String taskId, LocalDateTime endTime, String status) {
        String sql = "UPDATE import_tasks SET end_time = ?, status = ? WHERE task_id = ?";
        return jdbcTemplate.update(sql, endTime, status, taskId);
    }

    public ImportTask findById(String taskId) {
        String sql = "SELECT * FROM import_tasks WHERE task_id = ?";
        List<ImportTask> tasks = jdbcTemplate.query(sql, TASK_ROW_MAPPER, taskId);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    public List<ImportTask> findAll(int page, int size, String status) {
        int offset = (page - 1) * size;
        String sql;
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            sql = "SELECT * FROM import_tasks ORDER BY create_time DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            return jdbcTemplate.query(sql, TASK_ROW_MAPPER, offset, size);
        } else {
            sql = "SELECT * FROM import_tasks WHERE status = ? ORDER BY create_time DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            return jdbcTemplate.query(sql, TASK_ROW_MAPPER, status, offset, size);
        }
    }

    public int countAll(String status) {
        String sql;
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            sql = "SELECT COUNT(*) FROM import_tasks";
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } else {
            sql = "SELECT COUNT(*) FROM import_tasks WHERE status = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, status);
        }
    }

    public int deleteById(String taskId) {
        String sql = "DELETE FROM import_tasks WHERE task_id = ?";
        return jdbcTemplate.update(sql, taskId);
    }

    public List<ImportTask> findPendingTasks() {
        String sql = "SELECT * FROM import_tasks WHERE status = 'WAITING' ORDER BY create_time";
        return jdbcTemplate.query(sql, TASK_ROW_MAPPER);
    }

    public Map<String, Integer> getTaskStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", countAll(null));
        stats.put("waiting", countAll("WAITING"));
        stats.put("importing", countAll("IMPORTING"));
        stats.put("completed", countAll("COMPLETED"));
        stats.put("failed", countAll("FAILED"));
        stats.put("stopped", countAll("STOPPED"));
        return stats;
    }
}
