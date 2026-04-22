package com.oceanbase.importdata.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.oceanbase.jdbc.Driver");
        dataSource.setUrl("jdbc:oceanbase:oracle://120.55.98.148:2881/sys?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&connectTimeout=30000&socketTimeout=3600000&rewriteBatchedStatements=true&useServerPrepStmts=true&cachePrepStmts=true&prepStmtCacheSize=500&prepStmtCacheSqlLimit=2048&useLocalSessionState=true&useLocalTransactionState=true&useReadAheadInput=true&maintainTimeStats=false&elideSetAutoCommits=true&alwaysSendSetIsolation=false&useUnbufferedInput=false&defaultFetchSize=10000&dumpQueriesOnException=true&useFastIntParsing=true&cacheResultSetMetadata=true&cacheServerConfiguration=true");
        dataSource.setUsername("sys@oracle_db");
        dataSource.setPassword("change_on_install");
        dataSource.setInitialSize(20);
        dataSource.setMinIdle(20);
        dataSource.setMaxActive(100);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1 FROM DUAL");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(200);
        dataSource.setConnectionProperties("druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000;defaultRowPrefetch=10000;defaultBatchValue=100000;connectTimeout=30000;socketTimeout=3600000");
        dataSource.setRemoveAbandoned(false);
        dataSource.setRemoveAbandonedTimeout(3600);
        dataSource.setLogAbandoned(true);
        // 🔴 性能优化：异步关闭连接
        dataSource.setAsyncInit(true);
        dataSource.setUseUnfairLock(true);
        return dataSource;
    }

    @Bean(name = "importTaskExecutor")
    public Executor importTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ImportTask-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
