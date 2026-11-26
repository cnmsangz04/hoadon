package vn.hoadon.ai.knowledge.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.ai.knowledge.controller.KnowledgeDtos;
import vn.hoadon.ai.knowledge.persistence.KnowledgeCategoryEntity;
import vn.hoadon.ai.knowledge.persistence.KnowledgeCategoryRepository;
import vn.hoadon.ai.knowledge.persistence.KnowledgeDocumentRepository;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class KnowledgeCategoryService {
    private final KnowledgeCategoryRepository repository;
    private final KnowledgeDocumentRepository documentRepository;

    public KnowledgeCategoryService(KnowledgeCategoryRepository repository,
                                    KnowledgeDocumentRepository documentRepository) {
        this.repository = repository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeCategoryEntity> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    public KnowledgeCategoryEntity create(KnowledgeDtos.CategoryRequest request) {
        KnowledgeCategoryEntity entity = new KnowledgeCategoryEntity();
        apply(entity, request);
        return repository.save(entity);
    }

    public KnowledgeCategoryEntity update(long id, KnowledgeDtos.CategoryRequest request) {
        KnowledgeCategoryEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục kiến thức"));
        apply(entity, request);
        return repository.save(entity);
    }

    public void delete(long id) {
        if (!repository.existsById(id)) throw new IllegalArgumentException("Không tìm thấy danh mục kiến thức");
        if (documentRepository.countByCategoryId(id) > 0) {
            throw new IllegalArgumentException("Không thể xóa danh mục đang có tài liệu");
        }
        repository.deleteById(id);
    }

    private void apply(KnowledgeCategoryEntity entity, KnowledgeDtos.CategoryRequest request) {
        if (request == null || request.code() == null || !request.code().trim().matches("[A-Za-z0-9_-]{2,80}")) {
            throw new IllegalArgumentException("Mã danh mục chỉ gồm chữ, số, gạch ngang hoặc gạch dưới");
        }
        if (request.name() == null || request.name().isBlank() || request.name().trim().length() > 255) {
            throw new IllegalArgumentException("Tên danh mục không hợp lệ");
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        repository.findByCodeIgnoreCase(code).filter(found -> !found.getId().equals(entity.getId())).ifPresent(found -> {
            throw new IllegalArgumentException("Mã danh mục đã tồn tại");
        });
        entity.setCode(code);
        entity.setName(request.name().trim());
        entity.setDescription(trim(request.description(), 1000));
        entity.setActive(request.active() == null || request.active());
    }

    private String trim(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        return result.length() > max ? result.substring(0, max) : result;
    }
}
