package vn.hoadon.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import vn.hoadon.ai.config.AiProperties;
import vn.hoadon.ai.model.AiFunctionCall;
import vn.hoadon.ai.model.AiFunctionDefinition;
import vn.hoadon.ai.model.AiUsage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class GeminiAiProvider implements AiProvider {
    private static final Pattern MODEL_NAME = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final int MAX_RESPONSE_CHARACTERS = 2_000_000;

    private final AiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiAiProvider(AiProperties properties, HttpClient aiHttpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = aiHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(properties.getGemini().getApiKey());
    }

    @Override
    public AiProviderResponse generate(AiProviderRequest request) {
        validateConfiguration();
        HttpRequest httpRequest = createHttpRequest(request);
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response, request);
        } catch (HttpTimeoutException ex) {
            throw new AiProviderException("AI_TIMEOUT", "Gemini request timed out", true, 0, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI_INTERRUPTED", "Gemini request was interrupted", false, 0, ex);
        } catch (IOException ex) {
            throw new AiProviderException("AI_UNAVAILABLE", "Cannot connect to Gemini", true, 0, ex);
        }
    }

    private HttpRequest createHttpRequest(AiProviderRequest request) {
        String json;
        try {
            json = objectMapper.writeValueAsString(createRequestBody(request));
        } catch (JsonProcessingException ex) {
            throw new AiProviderException("AI_INVALID_REQUEST", "Cannot serialize Gemini request", false, 0, ex);
        }

        return HttpRequest.newBuilder(endpoint())
                .timeout(Duration.ofSeconds(Math.max(1, properties.getGemini().getRequestTimeoutSeconds())))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("x-goog-api-key", properties.getGemini().getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
    }

    private ObjectNode createRequestBody(AiProviderRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        if (StringUtils.hasText(request.systemInstruction())) {
            root.set("systemInstruction", contentWithText(null, request.systemInstruction()));
        }

        ArrayNode contents = root.putArray("contents");
        contents.add(contentWithText("user", request.prompt()));

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("maxOutputTokens", request.maxOutputTokens());
        if (request.temperature() != null) {
            generationConfig.put("temperature", request.temperature());
        }
        if (request.responseJsonSchema() != null) {
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.set("responseJsonSchema", request.responseJsonSchema());
        }
        if (!request.functions().isEmpty()) {
            addFunctions(root, request.functions());
        }
        return root;
    }

    private ObjectNode contentWithText(String role, String text) {
        ObjectNode content = objectMapper.createObjectNode();
        if (role != null) content.put("role", role);
        content.putArray("parts").addObject().put("text", text);
        return content;
    }

    private void addFunctions(ObjectNode root, List<AiFunctionDefinition> functions) {
        ArrayNode declarations = root.putArray("tools").addObject().putArray("functionDeclarations");
        for (AiFunctionDefinition function : functions) {
            ObjectNode declaration = declarations.addObject();
            declaration.put("name", function.name());
            if (StringUtils.hasText(function.description())) {
                declaration.put("description", function.description());
            }
            declaration.set("parametersJsonSchema", function.parametersJsonSchema());
        }
    }

    private AiProviderResponse handleResponse(HttpResponse<String> response, AiProviderRequest request) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw httpError(status, retryAfterMillis(response));
        }
        String body = response.body();
        if (body == null || body.isBlank() || body.length() > MAX_RESPONSE_CHARACTERS) {
            throw new AiProviderException("AI_INVALID_RESPONSE", "Gemini returned an invalid response", false);
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                String blockReason = root.path("promptFeedback").path("blockReason").asText();
                String code = blockReason.isBlank() ? "AI_INVALID_RESPONSE" : "AI_CONTENT_BLOCKED";
                throw new AiProviderException(code, "Gemini returned no candidate", false);
            }

            JsonNode candidate = candidates.get(0);
            ParsedContent parsed = parseContent(candidate.path("content").path("parts"));
            JsonNode structuredOutput = parseStructuredOutput(request, parsed.text());
            AiUsage usage = parseUsage(root.path("usageMetadata"));
            return new AiProviderResponse(parsed.text(), structuredOutput, parsed.functionCalls(), usage,
                    properties.getGemini().getModel(), candidate.path("finishReason").asText("UNSPECIFIED"));
        } catch (AiProviderException ex) {
            throw ex;
        } catch (JsonProcessingException ex) {
            throw new AiProviderException("AI_INVALID_RESPONSE", "Cannot parse Gemini response", false, 0, ex);
        }
    }

    private ParsedContent parseContent(JsonNode parts) {
        if (!parts.isArray()) {
            throw new AiProviderException("AI_INVALID_RESPONSE", "Gemini response has no content", false);
        }
        StringBuilder text = new StringBuilder();
        List<AiFunctionCall> functionCalls = new ArrayList<>();
        for (JsonNode part : parts) {
            if (part.has("text")) text.append(part.path("text").asText());
            JsonNode functionCall = part.path("functionCall");
            if (functionCall.isObject()) {
                String name = functionCall.path("name").asText();
                JsonNode arguments = functionCall.path("args");
                if (!name.isBlank() && arguments.isObject()) {
                    functionCalls.add(new AiFunctionCall(name, arguments.deepCopy()));
                }
            }
        }
        if (text.isEmpty() && functionCalls.isEmpty()) {
            throw new AiProviderException("AI_INVALID_RESPONSE", "Gemini response is empty", false);
        }
        return new ParsedContent(text.toString(), functionCalls);
    }

    private JsonNode parseStructuredOutput(AiProviderRequest request, String text) throws JsonProcessingException {
        if (request.responseJsonSchema() == null) return null;
        if (text == null || text.isBlank()) {
            throw new AiProviderException("AI_INVALID_RESPONSE", "Gemini structured response is empty", false);
        }
        JsonNode result = objectMapper.readTree(text);
        if (result == null || result.isMissingNode()) {
            throw new AiProviderException("AI_INVALID_RESPONSE", "Gemini structured response is invalid", false);
        }
        return result;
    }

    private AiUsage parseUsage(JsonNode usage) {
        long input = usage.path("promptTokenCount").asLong(0);
        long output = usage.path("candidatesTokenCount").asLong(0);
        long total = usage.path("totalTokenCount").asLong(input + output);
        return new AiUsage(input, output, total);
    }

    private AiProviderException httpError(int status, long retryAfterMs) {
        return switch (status) {
            case 400 -> new AiProviderException("AI_INVALID_REQUEST", "Gemini rejected the request", false);
            case 401, 403 -> new AiProviderException("AI_UNAUTHORIZED", "Gemini authentication failed", false);
            case 408 -> new AiProviderException("AI_TIMEOUT", "Gemini request timed out", true, retryAfterMs, null);
            case 429 -> new AiProviderException("AI_QUOTA_EXHAUSTED", "Gemini quota exhausted", true,
                    retryAfterMs, null);
            default -> new AiProviderException("AI_UNAVAILABLE", "Gemini is unavailable", status >= 500,
                    retryAfterMs, null);
        };
    }

    private long retryAfterMillis(HttpResponse<?> response) {
        String value = response.headers().firstValue("Retry-After").orElse("").trim();
        try {
            return Math.max(0, Long.parseLong(value) * 1_000);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private URI endpoint() {
        String baseUrl = properties.getGemini().getBaseUrl();
        String model = properties.getGemini().getModel();
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(model) || !MODEL_NAME.matcher(model).matches()) {
            throw new AiProviderException("AI_INVALID_CONFIGURATION", "Gemini configuration is invalid", false);
        }
        String normalizedBaseUrl = baseUrl.trim().replaceAll("/+$", "");
        URI endpoint;
        try {
            endpoint = URI.create(normalizedBaseUrl + "/models/" + model + ":generateContent");
        } catch (IllegalArgumentException ex) {
            throw new AiProviderException("AI_INVALID_CONFIGURATION", "Gemini endpoint is invalid", false, 0, ex);
        }
        String scheme = endpoint.getScheme() == null ? "" : endpoint.getScheme().toLowerCase(Locale.ROOT);
        if (!("https".equals(scheme) || "http".equals(scheme))) {
            throw new AiProviderException("AI_INVALID_CONFIGURATION", "Gemini endpoint scheme is invalid", false);
        }
        if ("http".equals(scheme) && !isLoopbackHost(endpoint.getHost())) {
            throw new AiProviderException("AI_INVALID_CONFIGURATION",
                    "Gemini endpoint must use HTTPS", false);
        }
        return endpoint;
    }

    private boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private void validateConfiguration() {
        if (!isConfigured()) {
            throw new AiProviderException("AI_NOT_CONFIGURED", "Gemini API key is missing", false);
        }
    }

    private record ParsedContent(String text, List<AiFunctionCall> functionCalls) {
    }
}
