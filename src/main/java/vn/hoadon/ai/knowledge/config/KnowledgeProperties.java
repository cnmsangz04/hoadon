package vn.hoadon.ai.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.knowledge")
public class KnowledgeProperties {
    private String indexPath = "data/ai-knowledge-index";
    private long maxUploadBytes = 10 * 1024 * 1024;
    private int maxExtractedCharacters = 2_000_000;
    private int maxPdfPages = 200;
    private int maxConcurrentProcessing = 2;
    private int chunkTargetCharacters = 2_400;
    private int chunkMaxCharacters = 5_000;
    private int maxRetrievalChunks = 8;
    private int maxContextCharacters = 16_000;

    public String getIndexPath() { return indexPath; }
    public void setIndexPath(String indexPath) { this.indexPath = indexPath; }
    public long getMaxUploadBytes() { return maxUploadBytes; }
    public void setMaxUploadBytes(long maxUploadBytes) { this.maxUploadBytes = maxUploadBytes; }
    public int getMaxExtractedCharacters() { return maxExtractedCharacters; }
    public void setMaxExtractedCharacters(int maxExtractedCharacters) { this.maxExtractedCharacters = maxExtractedCharacters; }
    public int getMaxPdfPages() { return maxPdfPages; }
    public void setMaxPdfPages(int maxPdfPages) { this.maxPdfPages = maxPdfPages; }
    public int getMaxConcurrentProcessing() { return maxConcurrentProcessing; }
    public void setMaxConcurrentProcessing(int maxConcurrentProcessing) { this.maxConcurrentProcessing = maxConcurrentProcessing; }
    public int getChunkTargetCharacters() { return chunkTargetCharacters; }
    public void setChunkTargetCharacters(int chunkTargetCharacters) { this.chunkTargetCharacters = chunkTargetCharacters; }
    public int getChunkMaxCharacters() { return chunkMaxCharacters; }
    public void setChunkMaxCharacters(int chunkMaxCharacters) { this.chunkMaxCharacters = chunkMaxCharacters; }
    public int getMaxRetrievalChunks() { return maxRetrievalChunks; }
    public void setMaxRetrievalChunks(int maxRetrievalChunks) { this.maxRetrievalChunks = maxRetrievalChunks; }
    public int getMaxContextCharacters() { return maxContextCharacters; }
    public void setMaxContextCharacters(int maxContextCharacters) { this.maxContextCharacters = maxContextCharacters; }
}
