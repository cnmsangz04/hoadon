package vn.hoadon.ai.ocr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.ocr")
public class OcrProperties {
    private boolean enabled;
    private String command = "tesseract";
    private String languages = "vie+eng";
    private String tempDirectory = "";
    private long maxUploadBytes = 10 * 1024 * 1024;
    private int maxPdfPages = 20;
    private int maxImageWidth = 8_000;
    private int maxImageHeight = 8_000;
    private long maxImagePixels = 40_000_000L;
    private int maxProcessedDimension = 3_000;
    private int renderDpi = 160;
    private int maxConcurrentTasks = 1;
    private int queueWaitSeconds = 5;
    private int timeoutSeconds = 90;
    private int maxOutputCharacters = 1_000_000;
    private int minPdfTextCharacters = 40;
    private double lowConfidenceThreshold = 0.75d;
    private int maxItems = 100;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }
    public String getTempDirectory() { return tempDirectory; }
    public void setTempDirectory(String tempDirectory) { this.tempDirectory = tempDirectory; }
    public long getMaxUploadBytes() { return maxUploadBytes; }
    public void setMaxUploadBytes(long maxUploadBytes) { this.maxUploadBytes = maxUploadBytes; }
    public int getMaxPdfPages() { return maxPdfPages; }
    public void setMaxPdfPages(int maxPdfPages) { this.maxPdfPages = maxPdfPages; }
    public int getMaxImageWidth() { return maxImageWidth; }
    public void setMaxImageWidth(int maxImageWidth) { this.maxImageWidth = maxImageWidth; }
    public int getMaxImageHeight() { return maxImageHeight; }
    public void setMaxImageHeight(int maxImageHeight) { this.maxImageHeight = maxImageHeight; }
    public long getMaxImagePixels() { return maxImagePixels; }
    public void setMaxImagePixels(long maxImagePixels) { this.maxImagePixels = maxImagePixels; }
    public int getMaxProcessedDimension() { return maxProcessedDimension; }
    public void setMaxProcessedDimension(int maxProcessedDimension) { this.maxProcessedDimension = maxProcessedDimension; }
    public int getRenderDpi() { return renderDpi; }
    public void setRenderDpi(int renderDpi) { this.renderDpi = renderDpi; }
    public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
    public void setMaxConcurrentTasks(int maxConcurrentTasks) { this.maxConcurrentTasks = maxConcurrentTasks; }
    public int getQueueWaitSeconds() { return queueWaitSeconds; }
    public void setQueueWaitSeconds(int queueWaitSeconds) { this.queueWaitSeconds = queueWaitSeconds; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxOutputCharacters() { return maxOutputCharacters; }
    public void setMaxOutputCharacters(int maxOutputCharacters) { this.maxOutputCharacters = maxOutputCharacters; }
    public int getMinPdfTextCharacters() { return minPdfTextCharacters; }
    public void setMinPdfTextCharacters(int minPdfTextCharacters) { this.minPdfTextCharacters = minPdfTextCharacters; }
    public double getLowConfidenceThreshold() { return lowConfidenceThreshold; }
    public void setLowConfidenceThreshold(double lowConfidenceThreshold) { this.lowConfidenceThreshold = lowConfidenceThreshold; }
    public int getMaxItems() { return maxItems; }
    public void setMaxItems(int maxItems) { this.maxItems = maxItems; }
}
