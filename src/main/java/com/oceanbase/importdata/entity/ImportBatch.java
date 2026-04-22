package com.oceanbase.importdata.entity;

import java.time.LocalDateTime;

public class ImportBatch {

    private String batchId;
    private String taskId;
    private Integer batchNumber;
    private Integer rowsCount;
    private Long startPosition;
    private Long endPosition;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;

    public ImportBatch() {}

    public ImportBatch(String taskId, Integer batchNumber, Long startPosition, Long endPosition) {
        this.taskId = taskId;
        this.batchNumber = batchNumber;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.rowsCount = (int)(endPosition - startPosition + 1);
        this.status = "PENDING";
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public Integer getBatchNumber() { return batchNumber; }
    public void setBatchNumber(Integer batchNumber) { this.batchNumber = batchNumber; }

    public Integer getRowsCount() { return rowsCount; }
    public void setRowsCount(Integer rowsCount) { this.rowsCount = rowsCount; }

    public Long getStartPosition() { return startPosition; }
    public void setStartPosition(Long startPosition) { this.startPosition = startPosition; }

    public Long getEndPosition() { return endPosition; }
    public void setEndPosition(Long endPosition) { this.endPosition = endPosition; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getElapsedTimeMillis() {
        if (startTime == null) return 0L;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }
}
