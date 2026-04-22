CREATE TABLE data_source_configs (
    config_id VARCHAR2(36) PRIMARY KEY,
    name VARCHAR2(255) NOT NULL,
    db_type VARCHAR2(50) DEFAULT 'oceanbase',
    host VARCHAR2(255) NOT NULL,
    port NUMBER(10) DEFAULT 2881,
    database VARCHAR2(128) DEFAULT 'sys',
    username VARCHAR2(255) NOT NULL,
    password VARCHAR2(1000),
    driver_class_name VARCHAR2(255),
    jdbc_url VARCHAR2(1000),
    is_default NUMBER(1) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE import_tasks (
    task_id VARCHAR2(36) PRIMARY KEY,
    file_name VARCHAR2(255) NOT NULL,
    target_table VARCHAR2(128) NOT NULL,
    data_source_config_id VARCHAR2(36),
    total_rows NUMBER(20) DEFAULT 0,
    imported_rows NUMBER(20) DEFAULT 0,
    status VARCHAR2(20) DEFAULT 'WAITING',
    file_encoding VARCHAR2(20) DEFAULT 'UTF-8',
    delimiter VARCHAR2(10) DEFAULT ',',
    skip_header NUMBER(1) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    error_message VARCHAR2(4000),
    batch_size NUMBER(10) DEFAULT 10000,
    thread_count NUMBER(3) DEFAULT 4,
    file_path VARCHAR2(500)
);

CREATE TABLE import_batches (
    batch_id VARCHAR2(36) PRIMARY KEY,
    task_id VARCHAR2(36) NOT NULL,
    batch_number NUMBER(10) NOT NULL,
    rows_count NUMBER(10) NOT NULL,
    start_position NUMBER(20) NOT NULL,
    end_position NUMBER(20) NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    error_message VARCHAR2(1000),
    CONSTRAINT fk_import_batches_task FOREIGN KEY (task_id) REFERENCES import_tasks(task_id)
);

CREATE TABLE test_data (
    id NUMBER(20),
    col_text_01 VARCHAR2(500),
    col_text_02 VARCHAR2(500),
    col_text_03 VARCHAR2(500),
    col_text_04 VARCHAR2(500),
    col_text_05 VARCHAR2(500),
    col_number_01 NUMBER(18,4),
    col_number_02 NUMBER(18,4),
    col_number_03 NUMBER(18,4),
    col_number_04 NUMBER(18,4),
    col_number_05 NUMBER(18,4),
    col_number_06 NUMBER(18,4),
    col_number_07 NUMBER(18,4),
    col_number_08 NUMBER(18,4),
    col_number_09 NUMBER(18,4),
    col_number_10 NUMBER(18,4),
    col_varchar_01 VARCHAR2(100),
    col_varchar_02 VARCHAR2(100),
    col_varchar_03 VARCHAR2(100),
    col_varchar_04 VARCHAR2(100),
    col_varchar_05 VARCHAR2(100)
);

CREATE INDEX idx_data_source_configs_name ON data_source_configs(name);
CREATE INDEX idx_import_tasks_status ON import_tasks(status);
CREATE INDEX idx_import_tasks_create_time ON import_tasks(create_time);
CREATE INDEX idx_import_batches_task_id ON import_batches(task_id);
CREATE INDEX idx_import_batches_status ON import_batches(status);
CREATE INDEX idx_test_data_id ON test_data(id);
