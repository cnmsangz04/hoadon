package vn.hoadon.ai.knowledge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, Long> {
    List<KnowledgeChunkEntity> findByVersionIdOrderByChunkIndexAsc(Long versionId);
    void deleteByDocumentId(Long documentId);
    void deleteByVersionId(Long versionId);
}
