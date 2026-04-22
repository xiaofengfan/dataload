package com.oceanbase.importdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "import")
public class ImportConfig {

    private Upload upload = new Upload();
    private Batch batch = new Batch();
    private Retry retry = new Retry();

    public static class Upload {
        private String path = "./uploads";
        private String maxFileSize = "10GB";

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(String maxFileSize) { this.maxFileSize = maxFileSize; }
    }

    public static class Batch {
        private int size = 10000;
        private int threadCount = 4;

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public int getThreadCount() { return threadCount; }
        public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    }

    public static class Retry {
        private int maxAttempts = 3;
        private long delayMs = 5000;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getDelayMs() { return delayMs; }
        public void setDelayMs(long delayMs) { this.delayMs = delayMs; }
    }

    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
}
