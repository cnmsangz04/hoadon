package vn.hoadon.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import vn.hoadon.ai.model.AiFeature;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private boolean enabled;
    private String provider = "gemini";
    private int maxInputChars = 50_000;
    private int defaultMaxOutputTokens = 2_048;
    private Retry retry = new Retry();
    private RateLimit rateLimit = new RateLimit();
    private Gemini gemini = new Gemini();
    private Features features = new Features();

    public boolean isFeatureEnabled(AiFeature feature) {
        return switch (feature) {
            case INVOICE_ASSISTANT -> features.invoiceAssistant;
            case DOCUMENT_RECOGNITION -> features.documentRecognition;
            case CHATBOT -> features.chatbot;
            case KNOWLEDGE_MANAGEMENT -> features.knowledgeManagement;
        };
    }

    public Map<String, Boolean> featureStates() {
        Map<String, Boolean> states = new LinkedHashMap<>();
        for (AiFeature feature : AiFeature.values()) {
            states.put(feature.getCode(), isFeatureEnabled(feature));
        }
        return states;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public int getMaxInputChars() { return maxInputChars; }
    public void setMaxInputChars(int maxInputChars) { this.maxInputChars = maxInputChars; }
    public int getDefaultMaxOutputTokens() { return defaultMaxOutputTokens; }
    public void setDefaultMaxOutputTokens(int defaultMaxOutputTokens) { this.defaultMaxOutputTokens = defaultMaxOutputTokens; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    public Gemini getGemini() { return gemini; }
    public void setGemini(Gemini gemini) { this.gemini = gemini; }
    public Features getFeatures() { return features; }
    public void setFeatures(Features features) { this.features = features; }

    public static class Retry {
        private int maxAttempts = 3;
        private long initialDelayMs = 500;
        private long maxDelayMs = 4_000;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getInitialDelayMs() { return initialDelayMs; }
        public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; }
        public long getMaxDelayMs() { return maxDelayMs; }
        public void setMaxDelayMs(long maxDelayMs) { this.maxDelayMs = maxDelayMs; }
    }

    public static class RateLimit {
        private int requestsPerMinute = 10;
        private int inputTokensPerMinute = 100_000;
        private int requestsPerDay = 400;
        private int requestsPerCompanyDay = 100;
        private int maxTrackedCompanies = 10_000;

        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
        public int getInputTokensPerMinute() { return inputTokensPerMinute; }
        public void setInputTokensPerMinute(int inputTokensPerMinute) { this.inputTokensPerMinute = inputTokensPerMinute; }
        public int getRequestsPerDay() { return requestsPerDay; }
        public void setRequestsPerDay(int requestsPerDay) { this.requestsPerDay = requestsPerDay; }
        public int getRequestsPerCompanyDay() { return requestsPerCompanyDay; }
        public void setRequestsPerCompanyDay(int requestsPerCompanyDay) { this.requestsPerCompanyDay = requestsPerCompanyDay; }
        public int getMaxTrackedCompanies() { return maxTrackedCompanies; }
        public void setMaxTrackedCompanies(int maxTrackedCompanies) { this.maxTrackedCompanies = maxTrackedCompanies; }
    }

    public static class Gemini {
        private String apiKey = "";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private String model = "gemini-3.5-flash-lite";
        private int connectTimeoutSeconds = 7;
        private int requestTimeoutSeconds = 30;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
        public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
        public void setRequestTimeoutSeconds(int requestTimeoutSeconds) { this.requestTimeoutSeconds = requestTimeoutSeconds; }
    }

    public static class Features {
        private boolean invoiceAssistant;
        private boolean documentRecognition;
        private boolean chatbot;
        private boolean knowledgeManagement;

        public boolean isInvoiceAssistant() { return invoiceAssistant; }
        public void setInvoiceAssistant(boolean invoiceAssistant) { this.invoiceAssistant = invoiceAssistant; }
        public boolean isDocumentRecognition() { return documentRecognition; }
        public void setDocumentRecognition(boolean documentRecognition) { this.documentRecognition = documentRecognition; }
        public boolean isChatbot() { return chatbot; }
        public void setChatbot(boolean chatbot) { this.chatbot = chatbot; }
        public boolean isKnowledgeManagement() { return knowledgeManagement; }
        public void setKnowledgeManagement(boolean knowledgeManagement) { this.knowledgeManagement = knowledgeManagement; }
    }
}
