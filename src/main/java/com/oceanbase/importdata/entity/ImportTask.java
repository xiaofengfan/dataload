package com.oceanbase.importdata.entity;

import java.time.LocalDateTime;

public class ImportTask {

    private String taskId;
    private String fileName;
    private String targetTable;
    private String dataSourceConfigId;
    private Long totalRows;
    private Long importedRows;
    private String status;
    private String fileEncoding;
    private String delimiter;
    private Integer skipHeader;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
    private Integer batchSize;
    private Integer threadCount;
    private String filePath;

    public ImportTask() {
        this.totalRows = 0L;
        this.importedRows = 0L;
        this.status = "WAITING";
        this.fileEncoding = "UTF-8";
        this.delimiter = ",";
        this.skipHeader = 0;
        this.batchSize = 5000;
        this.threadCount = 4;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }

    public String getDataSourceConfigId() { return dataSourceConfigId; }
    public void setDataSourceConfigId(String dataSourceConfigId) { this.dataSourceConfigId = dataSourceConfigId; }

    public Long getTotalRows() { return totalRows; }
    public void setTotalRows(Long totalRows) { this.totalRows = totalRows; }

    public Long getImportedRows() { return importedRows; }
    public void setImportedRows(Long importedRows) { this.importedRows = importedRows; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFileEncoding() { return fileEncoding; }
    public void setFileEncoding(String fileEncoding) { this.fileEncoding = fileEncoding; }

    public String getDelimiter() { return delimiter; }
    public void setDelimiter(String delimiter) { this.delimiter = delimiter; }

    public Integer getSkipHeader() { return skipHeader; }
    public void setSkipHeader(Integer skipHeader) { this.skipHeader = skipHeader; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }

    public Integer getThreadCount() { return threadCount; }
    public void setThreadCount(Integer threadCount) { this.threadCount = threadCount; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getProgressPercentage() {
        if (totalRows == null || totalRows == 0) return 0L;
        return (importedRows * 100) / totalRows;
    }

    public Long getElapsedTimeMillis() {
        if (startTime == null) return 0L;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }

    public String getFormattedElapsedTime() {
        long millis = getElapsedTimeMillis();
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
