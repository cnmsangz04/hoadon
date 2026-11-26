package vn.hoadon.ai.knowledge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeIndexBootstrap {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexBootstrap.class);
    private final KnowledgeDocumentService documentService;

    public KnowledgeIndexBootstrap(KnowledgeDocumentService documentService) {
        this.documentService = documentService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeIndex() {
        try {
            int chunks = documentService.rebuildIndex();
            log.info("AI knowledge index synchronized: chunks={}", chunks);
        } catch (RuntimeException ex) {
            log.warn("Cannot synchronize AI knowledge index at startup: {}", ex.getMessage());
        }
    }
}
