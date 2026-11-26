package vn.hoadon.ai.knowledge.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.ai.knowledge.config.KnowledgeProperties;
import vn.hoadon.ai.knowledge.model.KnowledgeDocumentStatus;
import vn.hoadon.ai.knowledge.model.KnowledgeReference;
import vn.hoadon.ai.knowledge.model.KnowledgeSearchHit;
import vn.hoadon.ai.knowledge.model.KnowledgeScope;
import vn.hoadon.ai.knowledge.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeRetrievalService {
    private final KnowledgeIndexService indexService;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDocumentVersionRepository versionRepository;
    private final KnowledgeSafetyService safetyService;
    private final KnowledgeProperties properties;

    public KnowledgeRetrievalService(KnowledgeIndexService indexService,
                                     KnowledgeChunkRepository chunkRepository,
                                     KnowledgeDocumentRepository documentRepository,
                                     KnowledgeDocumentVersionRepository versionRepository,
                                     KnowledgeSafetyService safetyService,
                                     KnowledgeProperties properties) {
        this.indexService = indexService;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.safetyService = safetyService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeReference> retrieve(Long companyId, String query, Integer requestedLimit, boolean externalOnly) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Nội dung truy xuất không được để trống");
        int limit = Math.min(properties.getMaxRetrievalChunks(), Math.max(1,
                requestedLimit == null ? properties.getMaxRetrievalChunks() : requestedLimit));
        List<KnowledgeSearchHit> hits = indexService.search(query, companyId, externalOnly, Math.max(limit * 3, limit));
        List<KnowledgeReference> result = new ArrayList<>();
        int totalCharacters = 0;
        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeSearchHit hit : hits) {
            KnowledgeChunkEntity chunk = chunkRepository.findById(hit.chunkId()).orElse(null);
            if (chunk == null) continue;
            KnowledgeDocumentEntity document = documentRepository.findById(chunk.getDocumentId()).orElse(null);
            if (!eligible(document, chunk, companyId, externalOnly)) continue;
            KnowledgeDocumentVersionEntity version = versionRepository.findById(chunk.getVersionId()).orElse(null);
            if (version == null || version.getStatus() != KnowledgeDocumentStatus.READY || !effective(version, now)) continue;
            String content = externalOnly ? safetyService.prepareForExternal(chunk.getContent()) : chunk.getContent();
            if (content.isBlank()) continue;
            if (totalCharacters + content.length() > properties.getMaxContextCharacters()) {
                int remaining = properties.getMaxContextCharacters() - totalCharacters;
                if (remaining < 200) break;
                content = content.substring(0, Math.min(content.length(), remaining));
            }
            result.add(new KnowledgeReference(document.getId(), version.getId(), chunk.getId(), document.getTitle(),
                    chunk.getHeading(), chunk.getSourceLocation(), content, hit.score()));
            totalCharacters += content.length();
            if (result.size() >= limit || totalCharacters >= properties.getMaxContextCharacters()) break;
        }
        return result;
    }

    private boolean eligible(KnowledgeDocumentEntity document, KnowledgeChunkEntity chunk, Long companyId,
                             boolean externalOnly) {
        if (document == null || !Boolean.TRUE.equals(document.getActive())
                || document.getStatus() != KnowledgeDocumentStatus.READY
                || !chunk.getVersionId().equals(document.getCurrentVersionId())) return false;
        if (externalOnly && !Boolean.TRUE.equals(document.getExternalShareAllowed())) return false;
        if (document.getScope() == KnowledgeScope.GLOBAL) return true;
        return companyId != null && companyId.equals(document.getCompanyId());
    }

    private boolean effective(KnowledgeDocumentVersionEntity version, LocalDateTime now) {
        return (version.getEffectiveFrom() == null || !version.getEffectiveFrom().isAfter(now))
                && (version.getEffectiveTo() == null || version.getEffectiveTo().isAfter(now));
    }
}
