# OceanBase 海量数据导入系统

## 项目简介

这是一个高效的 Web 数据导入系统，支持将 CSV/TXT 格式的海量数据（千万行级别）导入到 OceanBase Oracle 兼容模式数据库。

## 功能特性

- **支持多种文件格式**: CSV、TXT
- **海量数据处理**: 支持千万行级别数据导入
- **高效批量插入**: 每批次 10,000 行，峰值速度 5000-10000 行/秒
- **实时进度展示**: 实时显示导入进度、速度和预估剩余时间
- **任务管理**: 查看、取消、删除、重试导入任务
- **批次管理**: 支持单批次失败重试
- **执行时间记录**: 完整记录每个任务的执行时长和性能指标

## 技术栈

- **后端**: Java 17 + Spring Boot 3.2 + JDBC
- **数据库**: OceanBase Oracle 兼容模式
- **连接池**: Druid
- **前端**: 原生 HTML5 + CSS3 + JavaScript
- **构建工具**: Maven

## 项目结构

```
loaddata/
├── src/main/java/com/oceanbase/importdata/
│   ├── DataImportApplication.java      # 主应用入口
│   ├── config/                         # 配置类
│   │   ├── DataSourceConfig.java      # 数据库连接池配置
│   │   ├── ImportConfig.java          # 导入配置属性
│   │   └── CorsConfig.java            # CORS 配置
│   ├── controller/                     # REST API 控制器
│   │   ├── ImportController.java      # 导入 API
│   │   └── PageController.java        # 页面控制器
│   ├── entity/                         # 数据实体
│   │   ├── ImportTask.java            # 导入任务实体
│   │   └── ImportBatch.java           # 导入批次实体
│   ├── repository/                     # 数据访问层
│   │   ├── TaskRepository.java        # 任务仓库
│   │   └── BatchRepository.java       # 批次仓库
│   └── service/                        # 业务逻辑层
│       ├── ImportService.java         # 导入服务
│       └── FileService.java           # 文件服务
├── src/main/resources/
│   ├── static/index.html              # 前端页面
│   ├── application.yml                # 主配置文件
│   └── schema-oracle.sql              # 数据库初始化脚本
└── pom.xml                            # Maven 配置
```

## 数据库配置

连接信息：
- IP: 120.55.98.148
- 端口: 2881
- 用户: sys@oracle_db
- 密码: change_on_install
- 数据库: oracle_db

## 初始化数据库

在 OceanBase 数据库中执行 `schema-oracle.sql` 脚本创建所需的表：

```bash
# 连接到 OceanBase 数据库后执行
source schema-oracle.sql
```

## 构建项目

```bash
# 清理并打包
mvn clean package -DskipTests

# 或者只编译
mvn clean compile
```

## 运行项目

```bash
# 运行 JAR 包
java -jar target/data-import-system-1.0.0.jar

# 或者使用 Maven
mvn spring-boot:run
```

应用启动后访问: http://localhost:8080

## 配置说明

在 `application.yml` 中可以修改以下配置：

```yaml
# 数据库连接
spring:
  datasource:
    url: jdbc:oceanbase:oracle://120.55.98.148:2881/oracle_db
    username: sys@oracle_db
    password: change_on_install
    druid:
      max-active: 20  # 最大连接数

# 导入配置
import:
  upload:
    path: ./uploads              # 上传文件存储路径
    max-file-size: 10GB          # 最大文件大小
  batch:
    size: 10000                  # 每批次行数
    thread-count: 4              # 线程数
```

## API 接口

### 文件上传
- `POST /api/import/upload` - 上传文件并开始导入

### 任务管理
- `GET /api/import/tasks` - 获取任务列表
- `GET /api/import/tasks/{taskId}` - 获取任务详情
- `GET /api/import/tasks/{taskId}/progress` - 获取任务进度
- `POST /api/import/tasks/{taskId}/cancel` - 取消任务
- `DELETE /api/import/tasks/{taskId}` - 删除任务

### 批次管理
- `GET /api/import/batches/{taskId}` - 获取任务的批次列表
- `POST /api/import/batches/{batchId}/retry` - 重试失败的批次

## 使用说明

### 1. 创建导入任务

1. 输入目标表名（如 `TEST_DATA`）
2. 选择文件编码（默认 UTF-8）
3. 选择分隔符（默认逗号）
4. 如文件有表头，勾选"跳过首行"
5. 选择批量大小和线程数
6. 点击上传文件或拖拽文件到上传区域
7. 点击"开始导入"

### 2. 查看导入进度

- 任务列表显示所有导入任务
- 可按状态筛选任务
- 点击"详情"查看任务和批次详细信息
- 实时显示导入速度、进度和预估剩余时间

### 3. 任务操作

- **详情**: 查看任务完整信息和所有批次
- **重试**: 对失败的任务或批次进行重试
- **删除**: 删除已完成或失败的任务

## 性能优化

系统已实现以下优化策略：

1. **文件分片读取**: 防止内存溢出
2. **批量提交**: 每 10,000 行为一个事务批次
3. **异步处理**: 文件上传后立即返回，导入异步进行
4. **连接池**: 使用 Druid 连接池管理数据库连接
5. **批次并发**: 支持多批次并行导入

## 注意事项

1. 确保目标表已存在且结构匹配
2. CSV/TXT 文件需符合选择的分隔符格式
3. 大文件导入可能需要较长时间，请耐心等待
4. 如需中断导入，可点击"取消导入"按钮

## License

MIT License
