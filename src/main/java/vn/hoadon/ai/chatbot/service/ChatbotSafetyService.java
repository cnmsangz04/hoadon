package vn.hoadon.ai.chatbot.service;

import org.springframework.stereotype.Service;
import vn.hoadon.ai.knowledge.service.KnowledgeSafetyService;

import java.util.regex.Pattern;

@Service
public class ChatbotSafetyService {
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?iu)(system\\s*prompt|developer\\s*message|api[ _-]?key|mật\\s*khẩu|access\\s*token)"
                    + "|(?iu)(hiện|in|tiết lộ|show|reveal).{0,40}(cấu hình|prompt|instruction|chỉ dẫn|secret)"
                    + "|(?iu)(select|insert|update|delete|drop|alter)\\s+.{0,30}(from|into|table|database)");

    private final KnowledgeSafetyService knowledgeSafetyService;

    public ChatbotSafetyService(KnowledgeSafetyService knowledgeSafetyService) {
        this.knowledgeSafetyService = knowledgeSafetyService;
    }

    public void validateQuestion(String question) {
        if (knowledgeSafetyService.hasPromptInjection(question) || FORBIDDEN.matcher(question).find()) {
            throw new IllegalArgumentException("Câu hỏi chứa chỉ dẫn không an toàn hoặc nằm ngoài phạm vi hỗ trợ");
        }
    }
}
