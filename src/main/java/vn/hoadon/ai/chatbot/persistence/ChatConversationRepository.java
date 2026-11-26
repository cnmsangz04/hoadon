package vn.hoadon.ai.chatbot.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversationEntity, Long> {
    List<ChatConversationEntity> findByUserIdAndCompanyIdOrderByUpdatedAtDesc(Long userId, Long companyId,
                                                                              Pageable pageable);
    Optional<ChatConversationEntity> findByIdAndUserIdAndCompanyId(Long id, Long userId, Long companyId);
    long countByUserIdAndCompanyId(Long userId, Long companyId);
}
