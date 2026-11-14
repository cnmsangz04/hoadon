package vn.hoadon.invoice.template.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "invoice.template")
public class InvoiceTemplateProperties {
    private String storageRoot = "invoice-documents";
    private String profileVersion = "chromium-1.64.0-fonts-1";
    private int workerCount = 2;
    private int timeoutSeconds = 45;
    private int maxPages = 200;
    private int maxInputBytes = 10 * 1024 * 1024;

    public String getStorageRoot() { return storageRoot; }
    public void setStorageRoot(String storageRoot) { this.storageRoot = storageRoot; }
    public String getProfileVersion() { return profileVersion; }
    public void setProfileVersion(String profileVersion) { this.profileVersion = profileVersion; }
    public int getWorkerCount() { return workerCount; }
    public void setWorkerCount(int workerCount) { this.workerCount = Math.clamp(workerCount, 1, 8); }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = Math.clamp(timeoutSeconds, 5, 120); }
    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int maxPages) { this.maxPages = Math.clamp(maxPages, 1, 500); }
    public int getMaxInputBytes() { return maxInputBytes; }
    public void setMaxInputBytes(int maxInputBytes) { this.maxInputBytes = Math.clamp(maxInputBytes, 1024, 50 * 1024 * 1024); }
}
