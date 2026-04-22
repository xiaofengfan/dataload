# 海量数据文件导入系统 - 技术规格说明书

## 1. 项目概述

### 项目名称
OceanBase 海量数据导入系统

### 核心功能
实现通过Web页面导入海量数据（CSV/TXT格式）到 OceanBase Oracle 数据库，支持千万行级别数据的高效导入，具备导入任务管理和执行时间记录功能。

### 目标用户
数据库管理员、数据工程师需要进行大批量数据导入操作的人员

## 2. 技术架构

### 前端技术
- 原生 HTML5 + CSS3 + JavaScript (ES6+)
- 无框架依赖，轻量级实现
- 文件上传：FormData API + XMLHttpRequest
- 进度展示：Server-Sent Events (SSE) 或 轮询

### 后端技术
- Java 17
- Spring Boot 3.2.x
- Spring JDBC (高性能批量插入)
- Maven 构建
- JDBC 连接池：Druid

### 数据库
- OceanBase Oracle 兼容模式
- 连接信息：
  - IP: 120.55.98.148
  - 端口: 2881
  - 用户: sys@oracle_db
  - 密码: change_on_install

## 3. 功能模块

### 3.1 数据文件导入

#### 导入配置
- **目标表名**：用户输入目标数据库表名
- **文件编码**：支持 UTF-8、GBK、GB2312 自动检测
- **分隔符**：支持逗号、制表符、分号、自定义分隔符
- **首行处理**：跳过首行（当首行是表头时）
- **数据插入模式**：
  - INSERT：逐条插入
  - INSERT BATCH：批量插入（推荐千万级）

#### 文件要求
- 支持格式：CSV、TXT
- 文件大小：支持 GB 级别大文件
- 数据行数：支持千万行级别

#### 性能优化策略
1. **文件分片读取**：防止内存溢出
2. **多线程并行导入**：根据数据量自动调整线程数
3. **批量提交**：每10000行为一个批次提交
4. **事务管理**：每个批次独立事务，失败不影响已完成批次
5. **预处理**：提前验证数据格式，减少回滚

### 3.2 导入任务管理

#### 任务列表
- 任务ID
- 文件名
- 目标表名
- 总行数
- 已导入行数
- 进度百分比
- 状态（等待中、导入中、已完成、失败）
- 开始时间
- 结束时间
- 执行时长

#### 任务操作
- 查看任务详情
- 取消正在导入的任务
- 删除已完成任务记录
- 重新导入失败的批次

#### 实时进度
- 每秒更新进度
- 显示当前处理批次
- 显示导入速度（行/秒）
- 显示预估剩余时间

### 3.3 执行时间记录

#### 时间指标
- 任务创建时间
- 文件上传完成时间
- 导入开始时间
- 导入结束时间
- 总执行时长
- 每批次平均耗时
- 导入速度统计

#### 日志记录
- 导入开始/结束事件
- 每批次完成记录
- 错误和警告记录
- 性能指标记录

## 4. 数据库设计

### 导入任务表 (import_tasks)
```sql
CREATE TABLE import_tasks (
    task_id VARCHAR(36) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    target_table VARCHAR(128) NOT NULL,
    total_rows NUMBER(20) DEFAULT 0,
    imported_rows NUMBER(20) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'WAITING',
    file_encoding VARCHAR(20) DEFAULT 'UTF-8',
    delimiter VARCHAR(10) DEFAULT ',',
    skip_header NUMBER(1) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    error_message VARCHAR(1000),
    batch_size NUMBER(10) DEFAULT 10000,
    thread_count NUMBER(3) DEFAULT 4
);

-- Oracle兼容模式序列
CREATE SEQUENCE import_tasks_seq START WITH 1 INCREMENT BY 1;
```

### 导入批次记录表 (import_batches)
```sql
CREATE TABLE import_batches (
    batch_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    batch_number NUMBER(10) NOT NULL,
    rows_count NUMBER(10) NOT NULL,
    start_position NUMBER(20) NOT NULL,
    end_position NUMBER(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    error_message VARCHAR(1000),
    FOREIGN KEY (task_id) REFERENCES import_tasks(task_id)
);

CREATE SEQUENCE import_batches_seq START WITH 1 INCREMENT BY 1;
```

## 5. API 接口设计

### 5.1 任务管理

#### 创建导入任务
- **POST** `/api/import/tasks`
- **Request**: `{targetTable, fileName, fileEncoding, delimiter, skipHeader, batchSize, threadCount}`
- **Response**: `{taskId, status, message}`

#### 获取任务列表
- **GET** `/api/import/tasks`
- **Query**: `?page=1&size=20&status=ALL`
- **Response**: `{tasks: [], total, page, size}`

#### 获取任务详情
- **GET** `/api/import/tasks/{taskId}`
- **Response**: `{taskDetails, batches: []}`

#### 取消任务
- **POST** `/api/import/tasks/{taskId}/cancel`
- **Response**: `{success, message}`

#### 删除任务
- **DELETE** `/api/import/tasks/{taskId}`
- **Response**: `{success, message}`

#### 获取任务进度
- **GET** `/api/import/tasks/{taskId}/progress`
- **Response**: `{taskId, status, totalRows, importedRows, progress, speed, estimatedTime}`

### 5.2 文件上传

#### 上传文件
- **POST** `/api/import/upload`
- **Content-Type**: `multipart/form-data`
- **Request**: 文件 + 任务配置参数
- **Response**: `{taskId, fileName, totalRows, message}`

### 5.3 批次管理

#### 获取批次列表
- **GET** `/api/import/batches/{taskId}`
- **Response**: `{batches: []}`

#### 重试失败批次
- **POST** `/api/import/batches/{batchId}/retry`
- **Response**: `{success, batchId, message}`

## 6. 性能指标

### 导入速度目标
- 千万行数据：30-60分钟完成
- 峰值速度：5000-10000行/秒
- 内存占用：稳定在512MB以下

### 优化参数
- 批量大小：10000行/批次
- 线程数：根据CPU核心数动态调整（默认4）
- 连接池大小：10-20个连接
- Socket超时：300秒

## 7. 错误处理

### 文件错误
- 文件不存在：返回友好提示
- 文件格式错误：检测并提示正确的格式
- 文件编码错误：自动检测并转换编码

### 数据错误
- 数据格式不匹配：记录错误行，跳过继续
- 主键冲突：可选择跳过或更新
- 超长数据：自动截断并警告

### 系统错误
- 数据库连接失败：重试3次，间隔5秒
- 网络中断：自动保存进度，支持断点续传
- 内存溢出：减少批次大小，释放缓存

## 8. 前端页面设计

### 布局结构
```
+------------------------------------------+
|              页面标题                      |
+------------------------------------------+
|  [导入配置区]                              |
|  - 目标表名输入                            |
|  - 文件编码选择                           |
|  - 分隔符选择                             |
|  - 首行跳过选项                           |
+------------------------------------------+
|  [文件上传区]                              |
|  - 文件选择按钮                           |
|  - 文件信息显示                           |
|  - 上传进度条                             |
+------------------------------------------+
|  [导入按钮] [取消按钮]                     |
+------------------------------------------+
|  [任务列表区]                              |
|  - 任务表格（分页）                        |
|  - 操作按钮                               |
+------------------------------------------+
|  [任务详情弹窗]                            |
|  - 详细信息                               |
|  - 批次列表                               |
+------------------------------------------+
```

### 颜色方案
- 主色：#409EFF (蓝色)
- 成功：#67C23A (绿色)
- 警告：#E6A23C (橙色)
- 危险：#F56C6C (红色)
- 信息：#909399 (灰色)

### 响应式设计
- 最小宽度：800px
- 移动端适配：表格横向滚动

## 9. 安全考虑

### SQL注入防护
- 参数化查询
- 表名白名单验证
- 特殊字符过滤

### 文件安全
- 文件类型验证（.csv, .txt）
- 文件大小限制（可配置）
- 文件名 sanitization

### 数据安全
- 敏感信息加密存储
- 操作日志审计

## 10. 部署要求

### 环境要求
- JDK 17+
- 内存：4GB+
- 磁盘：预留足够空间存储上传文件
- 网络：稳定的数据库连接

### 配置项
- 数据库连接池大小
- 最大并发任务数
- 单文件最大大小
- 批量插入大小
- 线程池大小
