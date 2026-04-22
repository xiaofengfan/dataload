package com.oceanbase.importdata.repository;

import com.oceanbase.importdata.entity.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class DataSourceConfigRepository {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfigRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public DataSourceConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(DataSourceConfig config) {
        String sql = "INSERT INTO data_source_configs (" +
                "config_id, name, db_type, host, port, database, " +
                "username, password, driver_class_name, jdbc_url, is_default, create_time, update_time" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql,
                config.getConfigId(),
                config.getName(),
                config.getDbType(),
                config.getHost(),
                config.getPort(),
                config.getDatabase(),
                config.getUsername(),
                config.getPassword(),
                config.getDriverClassName(),
                config.getJdbcUrl(),
                config.getIsDefault() ? 1 : 0,
                config.getCreateTime(),
                config.getUpdateTime()
        );
    }

    public void update(DataSourceConfig config) {
        String sql = "UPDATE data_source_configs SET " +
                "name = ?, db_type = ?, host = ?, port = ?, database = ?, " +
                "username = ?, password = ?, driver_class_name = ?, jdbc_url = ?, " +
                "update_time = ? WHERE config_id = ?";
        
        jdbcTemplate.update(sql,
                config.getName(),
                config.getDbType(),
                config.getHost(),
                config.getPort(),
                config.getDatabase(),
                config.getUsername(),
                config.getPassword(),
                config.getDriverClassName(),
                config.getJdbcUrl(),
                config.getUpdateTime(),
                config.getConfigId()
        );
    }

    public void updateDefaultConfig(String configId) {
        // 先把所有的 is_default 设为 0
        jdbcTemplate.update("UPDATE data_source_configs SET is_default = 0");
        // 再把指定的 config_id 设为 1
        jdbcTemplate.update("UPDATE data_source_configs SET is_default = 1, update_time = ? WHERE config_id = ?",
                LocalDateTime.now(), configId);
    }

    public void deleteById(String configId) {
        jdbcTemplate.update("DELETE FROM data_source_configs WHERE config_id = ?", configId);
    }

    public DataSourceConfig findById(String configId) {
        String sql = "SELECT * FROM data_source_configs WHERE config_id = ?";
        List<DataSourceConfig> results = jdbcTemplate.query(sql, new DataSourceConfigRowMapper(), configId);
        return results.isEmpty() ? null : results.get(0);
    }

    public DataSourceConfig findDefault() {
        String sql = "SELECT * FROM data_source_configs WHERE is_default = 1";
        List<DataSourceConfig> results = jdbcTemplate.query(sql, new DataSourceConfigRowMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    public List<DataSourceConfig> findAll() {
        String sql = "SELECT * FROM data_source_configs ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new DataSourceConfigRowMapper());
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM data_source_configs";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private static class DataSourceConfigRowMapper implements RowMapper<DataSourceConfig> {
        @Override
        public DataSourceConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataSourceConfig config = new DataSourceConfig();
            config.setConfigId(rs.getString("config_id"));
            config.setName(rs.getString("name"));
            config.setDbType(rs.getString("db_type"));
            config.setHost(rs.getString("host"));
            config.setPort(rs.getInt("port"));
            config.setDatabase(rs.getString("database"));
            config.setUsername(rs.getString("username"));
            config.setPassword(rs.getString("password"));
            config.setDriverClassName(rs.getString("driver_class_name"));
            config.setJdbcUrl(rs.getString("jdbc_url"));
            config.setIsDefault(rs.getInt("is_default") == 1);
            config.setCreateTime(rs.getTimestamp("create_time") != null ?
                    rs.getTimestamp("create_time").toLocalDateTime() : null);
            config.setUpdateTime(rs.getTimestamp("update_time") != null ?
                    rs.getTimestamp("update_time").toLocalDateTime() : null);
            return config;
        }
    }
}
