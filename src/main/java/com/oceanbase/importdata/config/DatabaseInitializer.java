package com.oceanbase.importdata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDatabase() {
        log.info("Starting database initialization...");
        try {
            String schemaScript = loadSchemaScript();
            executeSchema(schemaScript);
            alterExistingTables();
            initializeDefaultDataSource();
            log.info("Database initialization completed successfully!");
        } catch (Exception e) {
            log.error("Database initialization failed: {}", e.getMessage(), e);
        }
    }

    private void alterExistingTables() {
        try {
            jdbcTemplate.execute("ALTER TABLE import_tasks MODIFY error_message VARCHAR2(4000)");
            log.debug("Altered import_tasks.error_message to VARCHAR2(4000)");
        } catch (Exception e) {
            log.debug("Could not alter import_tasks.error_message: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE import_batches MODIFY error_message VARCHAR2(4000)");
            log.debug("Altered import_batches.error_message to VARCHAR2(4000)");
        } catch (Exception e) {
            log.debug("Could not alter import_batches.error_message: {}", e.getMessage());
        }
        
        try {
            // Add data_source_config_id column to import_tasks if not exists
            jdbcTemplate.execute("ALTER TABLE import_tasks ADD data_source_config_id VARCHAR2(36)");
            log.debug("Altered import_tasks to add data_source_config_id");
        } catch (Exception e) {
            log.debug("Could not add data_source_config_id column: {}", e.getMessage());
        }
    }

    private void initializeDefaultDataSource() {
        try {
            // Check if any data source exists
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM data_source_configs", Integer.class);
            
            if (count == null || count == 0) {
                // Insert a default data source
                String insertSql = "INSERT INTO data_source_configs " +
                        "(config_id, name, db_type, host, port, database, username, password, driver_class_name, jdbc_url, is_default) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                jdbcTemplate.update(insertSql,
                        "default",
                        "Default OceanBase",
                        "oceanbase",
                        "120.55.98.148",
                        2881,
                        "sys",
                        "sys@oracle_db",
                        "change_on_install",
                        "com.oceanbase.jdbc.Driver",
                        "jdbc:oceanbase:oracle://120.55.98.148:2881/sys?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true",
                        1
                );
                
                log.info("Initialized default data source configuration");
            }
        } catch (Exception e) {
            log.debug("Could not initialize default data source: {}", e.getMessage(), e);
        }
    }

    private String loadSchemaScript() throws Exception {
        ClassPathResource resource = new ClassPathResource("schema-oracle.sql");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void executeSchema(String schemaScript) {
        String[] statements = splitStatements(schemaScript);

        for (String statement : statements) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("--")) continue;
            if (trimmed.toUpperCase().contains("DECLARE") || trimmed.toUpperCase().contains("BEGIN")) {
                executeBlock(trimmed);
            } else {
                executeStatement(trimmed);
            }
        }
    }

    private String[] splitStatements(String script) {
        return script.split(";(?=(?:[^']*'[^']*')*[^']*$)");
    }

    private void executeStatement(String sql) {
        if (sql.trim().isEmpty()) return;
        try {
            jdbcTemplate.execute(sql);
            log.debug("Executed: {}", truncateLog(sql));
        } catch (Exception e) {
            if (isIgnoreableError(e)) {
                log.debug("Ignored: {}", truncateLog(sql));
            } else {
                log.warn("Statement failed (may already exist): {} - {}", truncateLog(sql), e.getMessage());
            }
        }
    }

    private void executeBlock(String block) {
        if (block.trim().isEmpty()) return;
        try {
            jdbcTemplate.execute(block);
            log.debug("Executed block: {}", truncateLog(block));
        } catch (Exception e) {
            if (isIgnoreableError(e)) {
                log.debug("Ignored block");
            } else {
                log.warn("Block failed (may already exist): {}", e.getMessage());
            }
        }
    }

    private boolean isIgnoreableError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("ora-00942") ||
               msg.contains("already exists") ||
               msg.contains("duplicate") ||
               msg.contains("ora-01430") ||
               msg.contains("ora-01432");
    }

    private String truncateLog(String sql) {
        if (sql.length() > 100) {
            return sql.substring(0, 100) + "...";
        }
        return sql;
    }
}
