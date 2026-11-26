package vn.hoadon.ai.knowledge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, Long>,
        JpaSpecificationExecutor<KnowledgeDocumentEntity> {
    long countByCategoryId(Long categoryId);
}
