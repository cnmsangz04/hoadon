package vn.hoadon.ai.knowledge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategoryEntity, Long> {
    List<KnowledgeCategoryEntity> findAllByOrderByNameAsc();
    Optional<KnowledgeCategoryEntity> findByCodeIgnoreCase(String code);
}
