package vn.hoadon.ai.knowledge.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.ai.knowledge.config.KnowledgeProperties;
import vn.hoadon.ai.knowledge.controller.KnowledgeDtos;
import vn.hoadon.ai.knowledge.model.*;
import vn.hoadon.ai.knowledge.persistence.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class KnowledgeDocumentService {
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDocumentVersionRepository versionRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeCategoryRepository categoryRepository;
    private final KnowledgeFileValidator fileValidator;
    private final KnowledgeContentExtractor extractor;
    private final KnowledgeChunker chunker;
    private final KnowledgeSafetyService safetyService;
    private final KnowledgeIndexService indexService;
    private final Semaphore processingSlots;

    public KnowledgeDocumentService(KnowledgeDocumentRepository documentRepository,
                                    KnowledgeDocumentVersionRepository versionRepository,
                                    KnowledgeChunkRepository chunkRepository,
                                    KnowledgeCategoryRepository categoryRepository,
                                    KnowledgeFileValidator fileValidator,
                                    KnowledgeContentExtractor extractor,
                                    KnowledgeChunker chunker,
                                    KnowledgeSafetyService safetyService,
                                    KnowledgeIndexService indexService,
                                    KnowledgeProperties properties) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.chunkRepository = chunkRepository;
        this.categoryRepository = categoryRepository;
        this.fileValidator = fileValidator;
        this.extractor = extractor;
        this.chunker = chunker;
        this.safetyService = safetyService;
        this.indexService = indexService;
        this.processingSlots = new Semaphore(Math.max(1, properties.getMaxConcurrentProcessing()), true);
    }

    public Page<KnowledgeDocumentEntity> find(String keyword, Long categoryId, KnowledgeDocumentType type,
                                               KnowledgeDocumentStatus status, Boolean active, Long companyId,
                                               Pageable pageable) {
        Specification<KnowledgeDocumentEntity> specification = Specification.unrestricted();
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("title")), pattern));
        }
        if (categoryId != null) specification = specification.and((r, q, b) -> b.equal(r.get("categoryId"), categoryId));
        if (type != null) specification = specification.and((r, q, b) -> b.equal(r.get("documentType"), type));
        if (status != null) specification = specification.and((r, q, b) -> b.equal(r.get("status"), status));
        if (active != null) specification = specification.and((r, q, b) -> b.equal(r.get("active"), active));
        if (companyId != null) specification = specification.and((r, q, b) -> b.equal(r.get("companyId"), companyId));
        return documentRepository.findAll(specification, pageable);
    }

    public KnowledgeDocumentEntity createMarkdown(KnowledgeDtos.MarkdownRequest request, long userId) {
        validateMarkdownRequest(request);
        byte[] source = request.content().getBytes(StandardCharsets.UTF_8);
        KnowledgeDocumentEntity document = newDocument(request.title(), request.categoryId(), request.scope(),
                request.companyId(), request.externalShareAllowed(), KnowledgeDocumentType.MD, userId);
        documentRepository.save(document);
        createVersion(document, "knowledge-" + document.getId() + ".md", "text/markdown", source,
                request.effectiveFrom(), request.effectiveTo(), userId);
        return document;
    }

    public KnowledgeDocumentEntity upload(String title, Long categoryId, KnowledgeScope scope, Long companyId,
                                           Boolean externalShareAllowed, LocalDateTime effectiveFrom,
                                           LocalDateTime effectiveTo, MultipartFile file, long userId) {
        KnowledgeFileValidator.ValidatedFile validated = fileValidator.validate(file);
        KnowledgeDocumentEntity document = newDocument(title, categoryId, scope, companyId,
                externalShareAllowed, validated.type(), userId);
        documentRepository.save(document);
        createVersion(document, validated.filename(), validated.mimeType(), validated.bytes(),
                effectiveFrom, effectiveTo, userId);
        return document;
    }

    public KnowledgeDocumentEntity updateMarkdown(long documentId, String content, LocalDateTime effectiveFrom,
                                                    LocalDateTime effectiveTo, long userId) {
        KnowledgeDocumentEntity document = get(documentId);
        if (document.getDocumentType() != KnowledgeDocumentType.MD) {
            throw new IllegalArgumentException("Chỉ tài liệu Markdown mới được chỉnh sửa trực tiếp");
        }
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Nội dung Markdown không được để trống");
        createVersion(document, "knowledge-" + documentId + ".md", "text/markdown",
                content.getBytes(StandardCharsets.UTF_8), effectiveFrom, effectiveTo, userId);
        return document;
    }

    public KnowledgeDocumentEntity updateMetadata(long documentId, KnowledgeDtos.MetadataRequest request) {
        KnowledgeDocumentEntity document = get(documentId);
        applyMetadata(document, request.title(), request.categoryId(), request.scope(), request.companyId(),
                request.externalShareAllowed());
        validateEffectiveDates(request.effectiveFrom(), request.effectiveTo());
        KnowledgeDocumentVersionEntity version = currentVersion(document);
        if (version != null) {
            version.setEffectiveFrom(request.effectiveFrom());
            version.setEffectiveTo(request.effectiveTo());
            versionRepository.save(version);
        }
        documentRepository.save(document);
        syncIndex(document);
        return document;
    }

    public KnowledgeDocumentEntity setActive(long documentId, boolean active) {
        KnowledgeDocumentEntity document = get(documentId);
        if (active && document.getStatus() != KnowledgeDocumentStatus.READY) {
            throw new IllegalArgumentException("Chỉ tài liệu đã xử lý thành công mới được kích hoạt");
        }
        document.setActive(active);
        documentRepository.save(document);
        syncIndex(document);
        return document;
    }

    public KnowledgeDocumentEntity reprocess(long documentId, long userId) {
        KnowledgeDocumentEntity document = get(documentId);
        KnowledgeDocumentVersionEntity current = currentVersion(document);
        createVersion(document, current.getOriginalFilename(), current.getMimeType(), current.getSourceBlob(),
                current.getEffectiveFrom(), current.getEffectiveTo(), userId);
        return document;
    }

    public void delete(long documentId) {
        KnowledgeDocumentEntity document = get(documentId);
        indexService.deleteDocument(documentId);
        chunkRepository.deleteByDocumentId(documentId);
        versionRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentEntity get(long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu kiến thức"));
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentVersionEntity currentVersion(KnowledgeDocumentEntity document) {
        if (document.getCurrentVersionId() == null) return null;
        return versionRepository.findById(document.getCurrentVersionId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentVersionEntity> versions(long documentId) {
        get(documentId);
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunkEntity> chunks(long versionId) {
        return chunkRepository.findByVersionIdOrderByChunkIndexAsc(versionId);
    }

    public int rebuildIndex() {
        List<KnowledgeIndexDocument> all = new ArrayList<>();
        for (KnowledgeDocumentEntity document : documentRepository.findAll()) {
            if (!Boolean.TRUE.equals(document.getActive()) || document.getStatus() != KnowledgeDocumentStatus.READY) continue;
            KnowledgeDocumentVersionEntity version = currentVersion(document);
            if (version == null) continue;
            all.addAll(indexDocuments(document, version, chunks(version.getId())));
        }
        indexService.rebuild(all);
        return all.size();
    }

    private void createVersion(KnowledgeDocumentEntity document, String filename, String mimeType, byte[] source,
                               LocalDateTime effectiveFrom, LocalDateTime effectiveTo, long userId) {
        validateEffectiveDates(effectiveFrom, effectiveTo);
        boolean acquired = false;
        KnowledgeDocumentVersionEntity version = new KnowledgeDocumentVersionEntity();
        try {
            acquired = processingSlots.tryAcquire(5, TimeUnit.SECONDS);
            if (!acquired) throw new IllegalStateException("Hệ thống đang xử lý quá nhiều tài liệu, vui lòng thử lại");
            int number = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                    .map(value -> value.getVersionNumber() + 1).orElse(1);
            version.setDocumentId(document.getId());
            version.setVersionNumber(number);
            version.setOriginalFilename(fileValidator.safeFilename(filename));
            version.setMimeType(mimeType);
            version.setFileSize((long) source.length);
            version.setChecksum(checksum(source));
            version.setSourceBlob(source.clone());
            version.setStatus(KnowledgeDocumentStatus.PROCESSING);
            version.setEffectiveFrom(effectiveFrom);
            version.setEffectiveTo(effectiveTo);
            version.setCreatedBy(userId);
            versionRepository.save(version);

            document.setStatus(KnowledgeDocumentStatus.PROCESSING);
            documentRepository.save(document);
            process(document, version);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tác vụ xử lý tài liệu đã bị gián đoạn", ex);
        } finally {
            if (acquired) processingSlots.release();
        }
    }

    private void process(KnowledgeDocumentEntity document, KnowledgeDocumentVersionEntity version) {
        try {
            ExtractedKnowledge extracted = extractor.extract(document.getDocumentType(), version.getSourceBlob());
            version.setExtractedContent(extracted.content());
            version.setPromptInjectionDetected(safetyService.hasPromptInjection(extracted.content()));
            if (extracted.requiresOcr()) {
                version.setStatus(KnowledgeDocumentStatus.OCR_REQUIRED);
                document.setStatus(KnowledgeDocumentStatus.OCR_REQUIRED);
                document.setActive(false);
            } else {
                List<KnowledgeChunkDraft> drafts = chunker.chunk(extracted.content());
                if (drafts.isEmpty()) throw new IllegalArgumentException("Không trích xuất được nội dung có thể lập chỉ mục");
                version.setStatus(KnowledgeDocumentStatus.READY);
                versionRepository.save(version);
                List<KnowledgeChunkEntity> chunks = saveChunks(document.getId(), version.getId(), drafts);
                document.setCurrentVersionId(version.getId());
                document.setStatus(KnowledgeDocumentStatus.READY);
                documentRepository.save(document);
                if (Boolean.TRUE.equals(document.getActive())) {
                    indexService.replaceDocument(document.getId(), indexDocuments(document, version, chunks));
                } else {
                    indexService.deleteDocument(document.getId());
                }
                return;
            }
        } catch (RuntimeException ex) {
            version.setStatus(KnowledgeDocumentStatus.FAILED);
            version.setErrorMessage(safeError(ex));
            document.setStatus(KnowledgeDocumentStatus.FAILED);
            document.setActive(false);
            indexService.deleteDocument(document.getId());
        }
        versionRepository.save(version);
        document.setCurrentVersionId(version.getId());
        documentRepository.save(document);
    }

    private List<KnowledgeChunkEntity> saveChunks(long documentId, long versionId, List<KnowledgeChunkDraft> drafts) {
        List<KnowledgeChunkEntity> entities = new ArrayList<>(drafts.size());
        for (KnowledgeChunkDraft draft : drafts) {
            KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
            entity.setDocumentId(documentId);
            entity.setVersionId(versionId);
            entity.setChunkIndex(draft.index());
            entity.setHeading(draft.heading());
            entity.setSourceLocation(draft.sourceLocation());
            entity.setContent(draft.content());
            entity.setEstimatedTokens(Math.max(1, (draft.content().length() + 3) / 4));
            entities.add(entity);
        }
        return chunkRepository.saveAll(entities);
    }

    private void syncIndex(KnowledgeDocumentEntity document) {
        KnowledgeDocumentVersionEntity version = currentVersion(document);
        if (!Boolean.TRUE.equals(document.getActive()) || document.getStatus() != KnowledgeDocumentStatus.READY
                || version == null) {
            indexService.deleteDocument(document.getId());
            return;
        }
        indexService.replaceDocument(document.getId(), indexDocuments(document, version, chunks(version.getId())));
    }

    private List<KnowledgeIndexDocument> indexDocuments(KnowledgeDocumentEntity document,
                                                         KnowledgeDocumentVersionEntity version,
                                                         List<KnowledgeChunkEntity> chunks) {
        return chunks.stream().map(chunk -> new KnowledgeIndexDocument(chunk.getId(), document.getId(),
                version.getId(), document.getCompanyId(), Boolean.TRUE.equals(document.getExternalShareAllowed()),
                document.getTitle(), chunk.getHeading(), chunk.getContent())).toList();
    }

    private KnowledgeDocumentEntity newDocument(String title, Long categoryId, KnowledgeScope scope, Long companyId,
                                                  Boolean externalShareAllowed, KnowledgeDocumentType type, long userId) {
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setDocumentType(type);
        document.setCreatedBy(userId);
        document.setActive(false);
        document.setStatus(KnowledgeDocumentStatus.PROCESSING);
        applyMetadata(document, title, categoryId, scope, companyId, externalShareAllowed);
        return document;
    }

    private void applyMetadata(KnowledgeDocumentEntity document, String title, Long categoryId,
                               KnowledgeScope scope, Long companyId, Boolean externalShareAllowed) {
        if (title == null || title.isBlank() || title.trim().length() > 255) {
            throw new IllegalArgumentException("Tiêu đề tài liệu không hợp lệ");
        }
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("Danh mục kiến thức không tồn tại");
        }
        KnowledgeScope safeScope = scope == null ? KnowledgeScope.GLOBAL : scope;
        if (safeScope == KnowledgeScope.COMPANY && companyId == null) {
            throw new IllegalArgumentException("Tài liệu phạm vi doanh nghiệp phải chọn doanh nghiệp");
        }
        document.setTitle(title.trim());
        document.setCategoryId(categoryId);
        document.setScope(safeScope);
        document.setCompanyId(safeScope == KnowledgeScope.GLOBAL ? null : companyId);
        document.setExternalShareAllowed(Boolean.TRUE.equals(externalShareAllowed));
    }

    private void validateMarkdownRequest(KnowledgeDtos.MarkdownRequest request) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Nội dung Markdown không được để trống");
        }
    }

    private void validateEffectiveDates(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("Ngày hết hiệu lực phải sau ngày bắt đầu hiệu lực");
        }
    }

    public boolean isEffective(KnowledgeDocumentVersionEntity version, LocalDateTime now) {
        return (version.getEffectiveFrom() == null || !version.getEffectiveFrom().isAfter(now))
                && (version.getEffectiveTo() == null || version.getEffectiveTo().isAfter(now));
    }

    private String checksum(byte[] source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không hỗ trợ SHA-256", ex);
        }
    }

    private String safeError(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) return "Không thể xử lý tài liệu";
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
