package vn.hoadon.ai.knowledge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentVersionRepository extends JpaRepository<KnowledgeDocumentVersionEntity, Long> {
    List<KnowledgeDocumentVersionEntity> findByDocumentIdOrderByVersionNumberDesc(Long documentId);
    Optional<KnowledgeDocumentVersionEntity> findTopByDocumentIdOrderByVersionNumberDesc(Long documentId);
    void deleteByDocumentId(Long documentId);
}
