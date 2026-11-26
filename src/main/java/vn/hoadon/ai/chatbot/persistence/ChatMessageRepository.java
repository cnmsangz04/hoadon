package vn.hoadon.ai.chatbot.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByConversationIdOrderByIdAsc(Long conversationId);
    List<ChatMessageEntity> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);
    Optional<ChatMessageEntity> findByIdAndConversationIdAndUserIdAndCompanyId(Long id, Long conversationId,
                                                                               Long userId, Long companyId);
    void deleteByConversationId(Long conversationId);
}
