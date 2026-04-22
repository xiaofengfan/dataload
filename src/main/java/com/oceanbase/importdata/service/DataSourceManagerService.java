package com.oceanbase.importdata.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.oceanbase.importdata.entity.DataSourceConfig;
import com.oceanbase.importdata.repository.DataSourceConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DataSourceManagerService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceManagerService.class);

    private final DataSourceConfigRepository configRepository;
    private final Map<String, DruidDataSource> dataSourceCache = new ConcurrentHashMap<>();

    public DataSourceManagerService(DataSourceConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public DataSourceConfig createConfig(DataSourceConfig config) {
        String configId = UUID.randomUUID().toString();
        config.setConfigId(configId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        
        // 如果没有驱动类名和 JDBC URL，自动生成
        if (config.getDriverClassName() == null || config.getDriverClassName().isEmpty()) {
            config.setDriverClassName(guessDriverClassName(config.getDbType()));
        }
        if (config.getJdbcUrl() == null || config.getJdbcUrl().isEmpty()) {
            config.setJdbcUrl(buildJdbcUrl(config));
        }

        configRepository.insert(config);
        
        // 如果是默认数据源，清除其他默认标记
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            configRepository.updateDefaultConfig(configId);
        }

        return configRepository.findById(configId);
    }

    public DataSourceConfig updateConfig(DataSourceConfig config) {
        DataSourceConfig existing = configRepository.findById(config.getConfigId());
        if (existing != null) {
            config.setUpdateTime(LocalDateTime.now());
            
            // 如果没有驱动类名和 JDBC URL，自动生成
            if (config.getDriverClassName() == null || config.getDriverClassName().isEmpty()) {
                config.setDriverClassName(guessDriverClassName(config.getDbType()));
            }
            if (config.getJdbcUrl() == null || config.getJdbcUrl().isEmpty()) {
                config.setJdbcUrl(buildJdbcUrl(config));
            }

            configRepository.update(config);

            // 清除缓存
            removeFromCache(config.getConfigId());

            // 如果是默认数据源，清除其他默认标记
            if (Boolean.TRUE.equals(config.getIsDefault())) {
                configRepository.updateDefaultConfig(config.getConfigId());
            }
        }
        return configRepository.findById(config.getConfigId());
    }

    public void deleteConfig(String configId) {
        removeFromCache(configId);
        configRepository.deleteById(configId);
    }

    public DataSourceConfig getConfig(String configId) {
        return configRepository.findById(configId);
    }

    public DataSourceConfig getDefaultConfig() {
        return configRepository.findDefault();
    }

    public java.util.List<DataSourceConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    public void setDefaultConfig(String configId) {
        configRepository.updateDefaultConfig(configId);
    }

    public DataSource getDataSource(String configId) {
        return dataSourceCache.computeIfAbsent(configId, id -> {
            DataSourceConfig config = configRepository.findById(id);
            if (config == null) {
                throw new IllegalArgumentException("DataSource config not found: " + id);
            }
            return createDataSource(config);
        });
    }

    public void testConnection(String configId) throws Exception {
        DataSourceConfig config = configRepository.findById(configId);
        if (config == null) {
            throw new IllegalArgumentException("DataSource config not found: " + configId);
        }

        DruidDataSource dataSource = createDataSource(config);
        try {
            dataSource.getConnection().close();
        } finally {
            dataSource.close();
        }
    }

    private DruidDataSource createDataSource(DataSourceConfig config) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(config.getDriverClassName());
        dataSource.setUrl(config.getJdbcUrl());
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        
        // 连接池配置
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(60000);
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setValidationQuery("SELECT 1 FROM DUAL");
        
        // 针对 Oracle 的优化
        dataSource.setConnectionProperties("defaultRowPrefetch=1000;defaultBatchValue=50000");
        
        log.info("Created DataSource for: {}", config.getName());
        return dataSource;
    }

    private void removeFromCache(String configId) {
        DruidDataSource dataSource = dataSourceCache.remove(configId);
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private String guessDriverClassName(String dbType) {
        if (dbType == null) return "com.oceanbase.jdbc.Driver";
        switch (dbType.toLowerCase()) {
            case "oceanbase":
            case "oracle":
                return "com.oceanbase.jdbc.Driver";
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "postgresql":
                return "org.postgresql.Driver";
            default:
                return "com.oceanbase.jdbc.Driver";
        }
    }

    private String buildJdbcUrl(DataSourceConfig config) {
        String dbType = config.getDbType() != null ? config.getDbType().toLowerCase() : "oceanbase";
        String host = config.getHost() != null ? config.getHost() : "localhost";
        int port = config.getPort() != null ? config.getPort() : 2881;
        String database = config.getDatabase() != null ? config.getDatabase() : "sys";

        switch (dbType) {
            case "oceanbase":
                return "jdbc:oceanbase:oracle://" + host + ":" + port + "/" + database +
                       "?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true";
            case "oracle":
                return "jdbc:oracle:thin:@" + host + ":" + port + ":" + database;
            case "mysql":
                return "jdbc:mysql://" + host + ":" + port + "/" + database +
                       "?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai";
            case "postgresql":
                return "jdbc:postgresql://" + host + ":" + port + "/" + database;
            default:
                return "jdbc:oceanbase:oracle://" + host + ":" + port + "/" + database;
        }
    }
}
