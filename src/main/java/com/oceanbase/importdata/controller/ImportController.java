package com.oceanbase.importdata.controller;

import com.oceanbase.importdata.entity.DataSourceConfig;
import com.oceanbase.importdata.entity.ImportBatch;
import com.oceanbase.importdata.entity.ImportTask;
import com.oceanbase.importdata.service.DataSourceManagerService;
import com.oceanbase.importdata.service.FileService;
import com.oceanbase.importdata.service.ImportService;
import com.oceanbase.importdata.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "*")
public class ImportController {

    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    @Autowired
    private ImportService importService;

    @Autowired
    private FileService fileService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DataSourceManagerService dataSourceManagerService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetTable", required = false, defaultValue = "") String targetTable,
            @RequestParam(value = "dataSourceConfigId", required = false) String dataSourceConfigId,
            @RequestParam(value = "fileEncoding", required = false, defaultValue = "UTF-8") String fileEncoding,
            @RequestParam(value = "delimiter", required = false, defaultValue = ",") String delimiter,
            @RequestParam(value = "skipHeader", required = false, defaultValue = "0") Integer skipHeader,
            @RequestParam(value = "batchSize", required = false, defaultValue = "10000") Integer batchSize,
            @RequestParam(value = "threadCount", required = false, defaultValue = "4") Integer threadCount) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            String filename = file.getOriginalFilename();
            if (!fileService.isValidFileType(filename)) {
                response.put("success", false);
                response.put("message", "Invalid file type. Only CSV and TXT files are allowed");
                return ResponseEntity.badRequest().body(response);
            }

            if (targetTable == null || targetTable.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Target table name is required");
                return ResponseEntity.badRequest().body(response);
            }

            String sanitizedTable = targetTable.replaceAll("[^a-zA-Z0-9_]", "");
            if (!sanitizedTable.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                response.put("success", false);
                response.put("message", "Invalid table name format");
                return ResponseEntity.badRequest().body(response);
            }

            ImportTask task = new ImportTask();
            task.setTargetTable(sanitizedTable);
            task.setFileName(filename);
            task.setDataSourceConfigId(dataSourceConfigId);
            task.setFileEncoding(fileEncoding);
            task.setDelimiter(delimiter);
            task.setSkipHeader(skipHeader);
            task.setBatchSize(batchSize);
            task.setThreadCount(threadCount);

            String taskId = importService.createImportTask(task);
            task.setTaskId(taskId);

            String filePath = fileService.uploadFile(file, taskId);
            task.setFilePath(filePath);
            int rowsUpdated = taskRepository.update(task);
            log.info("File uploaded: {}, taskId: {}, table: {}, updateRows: {}", filename, taskId, sanitizedTable, rowsUpdated);

            if (rowsUpdated > 0) {
                importService.executeImport(taskId);
            } else {
                log.error("Failed to update task with file path, aborting import");
                response.put("success", false);
                response.put("message", "Failed to save file path");
                return ResponseEntity.internalServerError().body(response);
            }

            response.put("success", true);
            response.put("taskId", taskId);
            response.put("fileName", filename);
            response.put("message", "File uploaded and import started");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getTasks(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<ImportTask> tasks = importService.getTasks(page, size, status);
            int total = importService.getTaskCount(status);

            response.put("success", true);
            response.put("tasks", tasks);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Get tasks failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();

        try {
            ImportTask task = importService.getTask(taskId);
            if (task == null) {
                response.put("success", false);
                response.put("message", "Task not found");
                return ResponseEntity.notFound().build();
            }

            List<ImportBatch> batches = importService.getTaskBatches(taskId);

            response.put("success", true);
            response.put("task", task);
            response.put("batches", batches);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Get task failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/tasks/{taskId}/progress")
    public ResponseEntity<Map<String, Object>> getTaskProgress(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> progress = importService.getTaskProgress(taskId);
            if (progress == null) {
                response.put("success", false);
                response.put("message", "Task not found");
                return ResponseEntity.notFound().build();
            }

            response.put("success", true);
            response.putAll(progress);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Get progress failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();

        try {
            ImportTask task = importService.getTask(taskId);
            if (task == null) {
                response.put("success", false);
                response.put("message", "Task not found");
                return ResponseEntity.notFound().build();
            }

            if ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
                response.put("success", false);
                response.put("message", "Task already finished");
                return ResponseEntity.badRequest().body(response);
            }

            importService.cancelTask(taskId);

            response.put("success", true);
            response.put("message", "Task cancellation requested");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Cancel task failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();

        try {
            importService.deleteTask(taskId);

            response.put("success", true);
            response.put("message", "Task deleted");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Delete task failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/batches/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskBatches(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ImportBatch> batches = importService.getTaskBatches(taskId);
            List<ImportBatch> failedBatches = importService.getFailedBatches(taskId);

            response.put("success", true);
            response.put("batches", batches);
            response.put("failedBatches", failedBatches);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Get batches failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/datasources")
    public ResponseEntity<Map<String, Object>> getDataSources() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<DataSourceConfig> configs = dataSourceManagerService.getAllConfigs();
            DataSourceConfig defaultConfig = dataSourceManagerService.getDefaultConfig();
            
            response.put("success", true);
            response.put("datasources", configs);
            response.put("default", defaultConfig);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Get data sources failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/batches/{batchId}/retry")
    public ResponseEntity<Map<String, Object>> retryBatch(@PathVariable String batchId) {
        Map<String, Object> response = new HashMap<>();

        try {
            importService.retryBatch(batchId);

            response.put("success", true);
            response.put("message", "Batch retry started");
            response.put("batchId", batchId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Retry batch failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/tasks/stats")
    public ResponseEntity<Map<String, Object>> getTaskStats() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Integer> stats = importService.getTaskStats();
            response.put("success", true);
            response.put("stats", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Get task stats failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
