package vn.hoadon.ai.knowledge.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.ai.knowledge.model.*;
import vn.hoadon.ai.knowledge.persistence.*;
import vn.hoadon.ai.knowledge.service.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.security.UserRoles;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/administrator/ai/knowledge")
public class KnowledgeAdminController extends BaseController {
    private static final int MAX_PAGE_SIZE = 100;

    private final KnowledgeDocumentService documentService;
    private final KnowledgeCategoryService categoryService;
    private final KnowledgeMarkdownService markdownService;
    private final KnowledgeRetrievalService retrievalService;
    private final KnowledgeQuestionService questionService;

    public KnowledgeAdminController(KnowledgeDocumentService documentService,
                                    KnowledgeCategoryService categoryService,
                                    KnowledgeMarkdownService markdownService,
                                    KnowledgeRetrievalService retrievalService,
                                    KnowledgeQuestionService questionService) {
        this.documentService = documentService;
        this.categoryService = categoryService;
        this.markdownService = markdownService;
        this.retrievalService = retrievalService;
        this.questionService = questionService;
    }

    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> documents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) KnowledgeDocumentType type,
            @RequestParam(required = false) KnowledgeDocumentStatus status,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Page<KnowledgeDocumentEntity> result = documentService.find(q, categoryId, type, status, active, companyId,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt")));
        List<KnowledgeDtos.DocumentView> items = result.getContent().stream().map(value -> view(value, false)).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("current_page", safePage);
        response.put("per_page", safeSize);
        response.put("total", result.getTotalElements());
        response.put("last_page", Math.max(1, result.getTotalPages()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<KnowledgeDtos.DocumentView> document(@PathVariable long id) {
        requireAdmin();
        return ResponseEntity.ok(view(documentService.get(id), true));
    }

    @PostMapping("/documents/markdown")
    public ResponseEntity<KnowledgeDtos.DocumentView> createMarkdown(@RequestBody KnowledgeDtos.MarkdownRequest request) {
        UserEntity user = requireAdmin();
        KnowledgeDtos.MarkdownRequest globalRequest = new KnowledgeDtos.MarkdownRequest(
                request.title(), request.categoryId(), KnowledgeScope.GLOBAL, null,
                request.externalShareAllowed(), request.content(), request.effectiveFrom(), request.effectiveTo());
        return ResponseEntity.ok(view(documentService.createMarkdown(globalRequest, user.getId()), true));
    }

    @PostMapping(value = "/documents/upload", consumes = "multipart/form-data")
    public ResponseEntity<KnowledgeDtos.DocumentView> upload(
            @RequestParam String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") Boolean externalShareAllowed,
            @RequestParam(required = false) LocalDateTime effectiveFrom,
            @RequestParam(required = false) LocalDateTime effectiveTo,
            @RequestPart("file") MultipartFile file) {
        UserEntity user = requireAdmin();
        return ResponseEntity.ok(view(documentService.upload(title, categoryId, KnowledgeScope.GLOBAL, null,
                externalShareAllowed, effectiveFrom, effectiveTo, file, user.getId()), true));
    }

    @PutMapping("/documents/{id}/metadata")
    public ResponseEntity<KnowledgeDtos.DocumentView> updateMetadata(@PathVariable long id,
                                                                      @RequestBody KnowledgeDtos.MetadataRequest request) {
        requireAdmin();
        KnowledgeDtos.MetadataRequest globalRequest = new KnowledgeDtos.MetadataRequest(
                request.title(), request.categoryId(), KnowledgeScope.GLOBAL, null,
                request.externalShareAllowed(), request.effectiveFrom(), request.effectiveTo());
        return ResponseEntity.ok(view(documentService.updateMetadata(id, globalRequest), true));
    }

    @PutMapping("/documents/{id}/markdown")
    public ResponseEntity<KnowledgeDtos.DocumentView> updateMarkdown(@PathVariable long id,
                                                                      @RequestBody KnowledgeDtos.MarkdownRequest request) {
        UserEntity user = requireAdmin();
        return ResponseEntity.ok(view(documentService.updateMarkdown(id, request.content(), request.effectiveFrom(),
                request.effectiveTo(), user.getId()), true));
    }

    @PostMapping("/documents/{id}/activate")
    public ResponseEntity<KnowledgeDtos.DocumentView> activate(@PathVariable long id) {
        requireAdmin();
        return ResponseEntity.ok(view(documentService.setActive(id, true), true));
    }

    @PostMapping("/documents/{id}/deactivate")
    public ResponseEntity<KnowledgeDtos.DocumentView> deactivate(@PathVariable long id) {
        requireAdmin();
        return ResponseEntity.ok(view(documentService.setActive(id, false), true));
    }

    @PostMapping("/documents/{id}/reprocess")
    public ResponseEntity<KnowledgeDtos.DocumentView> reprocess(@PathVariable long id) {
        UserEntity user = requireAdmin();
        return ResponseEntity.ok(view(documentService.reprocess(id, user.getId()), true));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        requireAdmin();
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/documents/{id}/versions")
    public ResponseEntity<List<KnowledgeDtos.VersionView>> versions(@PathVariable long id) {
        requireAdmin();
        return ResponseEntity.ok(documentService.versions(id).stream().map(this::versionView).toList());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<KnowledgeCategoryEntity>> categories() {
        requireAdmin();
        return ResponseEntity.ok(categoryService.findAll());
    }

    @PostMapping("/categories")
    public ResponseEntity<KnowledgeCategoryEntity> createCategory(@RequestBody KnowledgeDtos.CategoryRequest request) {
        requireAdmin();
        return ResponseEntity.ok(categoryService.create(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<KnowledgeCategoryEntity> updateCategory(@PathVariable long id,
                                                                   @RequestBody KnowledgeDtos.CategoryRequest request) {
        requireAdmin();
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable long id) {
        requireAdmin();
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/preview")
    public ResponseEntity<Map<String, String>> preview(@RequestBody Map<String, String> body) {
        requireAdmin();
        return ResponseEntity.ok(Map.of("html", markdownService.renderSafeHtml(body.get("content"))));
    }

    @PostMapping("/retrieve")
    public ResponseEntity<KnowledgeDtos.RetrievalResponse> retrieve(@RequestBody KnowledgeDtos.RetrievalRequest request) {
        requireAdmin();
        List<KnowledgeReference> sources = retrievalService.retrieve(request.companyId(), request.query(),
                request.limit(), Boolean.TRUE.equals(request.externalOnly()));
        return ResponseEntity.ok(new KnowledgeDtos.RetrievalResponse(request.query(), sources));
    }

    @PostMapping("/ask")
    public ResponseEntity<KnowledgeDtos.AnswerResponse> ask(@RequestBody KnowledgeDtos.AskRequest request) {
        UserEntity user = requireAdmin();
        Long companyId = request.companyId() == null ? user.getCompanyId() : request.companyId();
        if (companyId == null) throw new IllegalArgumentException("Tài khoản chưa có phạm vi doanh nghiệp để kiểm thử AI");
        if (!companyId.equals(user.getCompanyId())) {
            throw new AccessDeniedException("Chỉ được kiểm thử AI trong phạm vi doanh nghiệp của tài khoản");
        }
        return ResponseEntity.ok(questionService.answer(user.getId(), companyId, request.question()));
    }

    @PostMapping("/index/rebuild")
    public ResponseEntity<Map<String, Object>> rebuild() {
        requireAdmin();
        int chunks = documentService.rebuildIndex();
        return ResponseEntity.ok(Map.of("message", "Đã xây dựng lại chỉ mục kiến thức", "chunks", chunks));
    }

    private KnowledgeDtos.DocumentView view(KnowledgeDocumentEntity document, boolean includeContent) {
        KnowledgeDocumentVersionEntity version = documentService.currentVersion(document);
        List<KnowledgeChunkEntity> chunks = version == null ? List.of() : documentService.chunks(version.getId());
        return new KnowledgeDtos.DocumentView(document.getId(), document.getTitle(), document.getCompanyId(),
                document.getCategoryId(), document.getDocumentType(), document.getScope(),
                Boolean.TRUE.equals(document.getActive()), Boolean.TRUE.equals(document.getExternalShareAllowed()),
                document.getStatus(), document.getCurrentVersionId(), version == null ? null : version.getVersionNumber(),
                version == null ? null : version.getOriginalFilename(),
                version != null && Boolean.TRUE.equals(version.getPromptInjectionDetected()), chunks.size(),
                includeContent && version != null ? version.getExtractedContent() : null,
                version == null ? null : version.getEffectiveFrom(), version == null ? null : version.getEffectiveTo(),
                document.getCreatedAt(), document.getUpdatedAt());
    }

    private KnowledgeDtos.VersionView versionView(KnowledgeDocumentVersionEntity version) {
        return new KnowledgeDtos.VersionView(version.getId(), version.getVersionNumber(), version.getOriginalFilename(),
                version.getMimeType(), version.getFileSize(), version.getChecksum(), version.getStatus(),
                Boolean.TRUE.equals(version.getPromptInjectionDetected()), version.getErrorMessage(),
                version.getPageCount(), Boolean.TRUE.equals(version.getOcrUsed()),
                version.getOcrAverageConfidence() == null ? null : version.getOcrAverageConfidence().doubleValue(),
                version.getEffectiveFrom(), version.getEffectiveTo(), version.getCreatedAt());
    }

    private UserEntity requireAdmin() {
        UserEntity user = currentUser();
        if (user == null || !UserRoles.canAccessAdminArea(user.getRole())) {
            throw new AccessDeniedException("Chỉ quản trị viên hệ thống mới được quản lý thư viện tài liệu AI");
        }
        return user;
    }
}
