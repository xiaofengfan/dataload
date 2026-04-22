package com.oceanbase.importdata.repository;

import com.oceanbase.importdata.entity.ImportBatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class BatchRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<ImportBatch> BATCH_ROW_MAPPER = new RowMapper<ImportBatch>() {
        @Override
        public ImportBatch mapRow(ResultSet rs, int rowNum) throws SQLException {
            ImportBatch batch = new ImportBatch();
            batch.setBatchId(rs.getString("batch_id"));
            batch.setTaskId(rs.getString("task_id"));
            batch.setBatchNumber(rs.getInt("batch_number"));
            batch.setRowsCount(rs.getInt("rows_count"));
            batch.setStartPosition(rs.getLong("start_position"));
            batch.setEndPosition(rs.getLong("end_position"));
            batch.setStatus(rs.getString("status"));
            batch.setStartTime(toLocalDateTime(rs, "start_time"));
            batch.setEndTime(toLocalDateTime(rs, "end_time"));
            batch.setErrorMessage(rs.getString("error_message"));
            return batch;
        }

        private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
            java.sql.Timestamp ts = rs.getTimestamp(column);
            return ts != null ? ts.toLocalDateTime() : null;
        }
    };

    public String insert(ImportBatch batch) {
        if (batch.getBatchId() == null) {
            batch.setBatchId(UUID.randomUUID().toString());
        }
        String sql = "INSERT INTO import_batches (batch_id, task_id, batch_number, rows_count, " +
                "start_position, end_position, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, batch.getBatchId(), batch.getTaskId(), batch.getBatchNumber(),
                batch.getRowsCount(), batch.getStartPosition(), batch.getEndPosition(), batch.getStatus());
        return batch.getBatchId();
    }

    public int updateStatus(String batchId, String status, String errorMessage) {
        String sql = "UPDATE import_batches SET status = ?, error_message = ? WHERE batch_id = ?";
        return jdbcTemplate.update(sql, status, errorMessage, batchId);
    }

    public int updateStartTime(String batchId, LocalDateTime startTime) {
        String sql = "UPDATE import_batches SET start_time = ?, status = 'PROCESSING' WHERE batch_id = ?";
        return jdbcTemplate.update(sql, startTime, batchId);
    }

    public int updateEndTime(String batchId, LocalDateTime endTime, String status) {
        String sql = "UPDATE import_batches SET end_time = ?, status = ? WHERE batch_id = ?";
        return jdbcTemplate.update(sql, endTime, status, batchId);
    }

    public ImportBatch findById(String batchId) {
        String sql = "SELECT * FROM import_batches WHERE batch_id = ?";
        List<ImportBatch> batches = jdbcTemplate.query(sql, BATCH_ROW_MAPPER, batchId);
        return batches.isEmpty() ? null : batches.get(0);
    }

    public List<ImportBatch> findByTaskId(String taskId) {
        String sql = "SELECT * FROM import_batches WHERE task_id = ? ORDER BY batch_number";
        return jdbcTemplate.query(sql, BATCH_ROW_MAPPER, taskId);
    }

    public List<ImportBatch> findFailedByTaskId(String taskId) {
        String sql = "SELECT * FROM import_batches WHERE task_id = ? AND status = 'FAILED' ORDER BY batch_number";
        return jdbcTemplate.query(sql, BATCH_ROW_MAPPER, taskId);
    }

    public int deleteByTaskId(String taskId) {
        String sql = "DELETE FROM import_batches WHERE task_id = ?";
        return jdbcTemplate.update(sql, taskId);
    }

    public int countByTaskIdAndStatus(String taskId, String status) {
        String sql = "SELECT COUNT(*) FROM import_batches WHERE task_id = ? AND status = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, taskId, status);
    }
}
