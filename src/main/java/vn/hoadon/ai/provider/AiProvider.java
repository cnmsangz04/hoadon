package vn.hoadon.ai.provider;

public interface AiProvider {
    String name();
    boolean isConfigured();
    AiProviderResponse generate(AiProviderRequest request);
}
