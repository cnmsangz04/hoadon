package vn.hoadon.ai.chatbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.ai.chatbot.service.ChatbotService;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.UserEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/ai/chatbot")
public class ChatbotController extends BaseController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatbotDtos.ConversationView>> conversations() {
        UserEntity user = requireScopedUser();
        return ResponseEntity.ok(chatbotService.conversations(user.getId(), user.getCompanyId()));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ChatbotDtos.ConversationView> create(@RequestBody(required = false) Map<String, String> body) {
        UserEntity user = requireScopedUser();
        return ResponseEntity.ok(chatbotService.createConversation(user.getId(), user.getCompanyId(),
                body == null ? null : body.get("title")));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<ChatbotDtos.MessageView>> messages(@PathVariable long id) {
        UserEntity user = requireScopedUser();
        return ResponseEntity.ok(chatbotService.messages(id, user.getId(), user.getCompanyId()));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        UserEntity user = requireScopedUser();
        chatbotService.deleteConversation(id, user.getId(), user.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatbotDtos.AskResponse> ask(@RequestBody ChatbotDtos.AskRequest request) {
        UserEntity user = requireScopedUser();
        return ResponseEntity.ok(chatbotService.ask(user.getId(), user.getCompanyId(), request.conversationId(),
                request.question()));
    }

    @PostMapping("/conversations/{conversationId}/messages/{messageId}/feedback")
    public ResponseEntity<ChatbotDtos.MessageView> feedback(@PathVariable long conversationId,
                                                             @PathVariable long messageId,
                                                             @RequestBody ChatbotDtos.FeedbackRequest request) {
        UserEntity user = requireScopedUser();
        return ResponseEntity.ok(chatbotService.feedback(conversationId, messageId, user.getId(),
                user.getCompanyId(), request.helpful(), request.feedback()));
    }

    private UserEntity requireScopedUser() {
        UserEntity user = currentUser();
        if (user == null || user.getId() == null || user.getCompanyId() == null) {
            throw new IllegalArgumentException("Không xác định được tài khoản hoặc doanh nghiệp");
        }
        return user;
    }
}
