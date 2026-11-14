package vn.hoadon.invoice.template.render;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.service.TemplateException;
import vn.hoadon.invoice.template.storage.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InvoiceRenderWorker {
    private static final Logger log = LoggerFactory.getLogger(InvoiceRenderWorker.class);
    private final InvoiceRenderJobRepository jobs;
    private final InvoiceRenderArtifactRepository artifacts;
    private final InvoicePdfRenderer renderer;
    private final InvoiceAssetService assets;
    private final DocumentStorage storage;
    private final InvoiceTemplateProperties properties;
    private final TransactionTemplate transactions;
    private final ExecutorService executor;
    private final AtomicInteger running = new AtomicInteger();
    private final Semaphore slots;

    public InvoiceRenderWorker(InvoiceRenderJobRepository jobs, InvoiceRenderArtifactRepository artifacts, InvoicePdfRenderer renderer,
                                InvoiceAssetService assets, DocumentStorage storage, InvoiceTemplateProperties properties, PlatformTransactionManager manager) {
        this.jobs = jobs;
        this.artifacts = artifacts;
        this.renderer = renderer;
        this.assets = assets;
        this.storage = storage;
        this.properties = properties;
        this.transactions = new TransactionTemplate(manager);
        this.slots = new Semaphore(properties.getWorkerCount());
        this.executor = Executors.newFixedThreadPool(properties.getWorkerCount(), Thread.ofPlatform().name("invoice-render-", 0).factory());
    }

    @Scheduled(fixedDelayString = "${invoice.template.poll-delay-ms:500}")
    public void poll() {
        int available = properties.getWorkerCount() - running.get();
        if (available <= 0) return;
        for (Long id : jobs.findAvailable(now(), PageRequest.of(0, available))) {
            if (!slots.tryAcquire()) break;
            String token = UUID.randomUUID().toString();
            Boolean claimed = transactions.execute(status -> jobs.claim(id, token, now().plusSeconds(properties.getTimeoutSeconds() * 3L + 30), now()) == 1);
            if (!Boolean.TRUE.equals(claimed)) { slots.release(); continue; }
            running.incrementAndGet();
            executor.submit(() -> { try { execute(id, token); } finally { running.decrementAndGet(); slots.release(); } });
        }
    }

    public void renderNow(Long id) {
        try {
            if (!slots.tryAcquire(properties.getTimeoutSeconds(), TimeUnit.SECONDS)) throw TemplateException.invalid("RENDER_BUSY", "Hệ thống đang tạo nhiều bản in; vui lòng thử lại");
            try {
                String token = UUID.randomUUID().toString();
                Boolean claimed = transactions.execute(status -> jobs.claim(id, token, now().plusSeconds(properties.getTimeoutSeconds() * 3L + 30), now()) == 1);
                if (Boolean.TRUE.equals(claimed)) execute(id, token);
            } finally { slots.release(); }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw TemplateException.invalid("RENDER_INTERRUPTED", "Yêu cầu tạo bản in đã bị ngắt");
        }
    }

    void execute(Long id, String token) {
        try {
            var job = jobs.findById(id).orElseThrow();
            if (job.getAttemptCount() > 3) throw TemplateException.invalid("RENDER_RETRY_LIMIT", "Đã vượt giới hạn thử tạo PDF");
            if (!properties.getProfileVersion().equals(job.getProfileVersion())) throw TemplateException.invalid("RENDER_PROFILE_UNAVAILABLE", "Worker không có render profile của tài liệu");
            var result = renderer.render(job.getSourceContent(), assets.resources(job.getManifestJson(), job.getScope(), job.getCompanyId()));
            String key = storage.write(result.bytes());
            transactions.executeWithoutResult(status -> {
                var current = jobs.lockById(id).orElseThrow();
                if (!"RUNNING".equals(current.getState()) || !token.equals(current.getClaimToken())) return;
                var artifact = new InvoiceRenderArtifactEntity();
                artifact.setJobId(id);
                artifact.setScope(current.getScope());
                artifact.setCompanyId(current.getCompanyId());
                artifact.setTemplateVersionId(current.getTemplateVersionId());
                artifact.setSnapshotId(current.getSnapshotId());
                artifact.setInputHash(current.getInputHash());
                artifact.setStorageKey(key);
                artifact.setSha256(DocumentStorage.sha256(result.bytes()));
                artifact.setByteSize((long) result.bytes().length);
                artifact.setPageCount(result.pageCount());
                artifact.setProfileVersion(current.getProfileVersion());
                artifact.setCreatedAt(now());
                artifact = artifacts.saveAndFlush(artifact);
                current.setArtifactId(artifact.getId());
                current.setState("SUCCEEDED");
                current.setCompletedAt(now());
                current.setLeaseUntil(null);
            });
        } catch (Exception ex) {
            String code = ex instanceof TemplateException error ? error.getCode() : "RENDER_FAILED";
            String message = ex instanceof TemplateException ? ex.getMessage() : "Không tạo được PDF; cần kiểm tra nhật ký worker";
            log.warn("Invoice render failed: jobId={}, code={}", id, code, ex);
            transactions.executeWithoutResult(status -> {
                var current = jobs.lockById(id).orElseThrow();
                if (!"RUNNING".equals(current.getState()) || !token.equals(current.getClaimToken())) return;
                current.setState("FAILED");
                current.setErrorCode(code);
                current.setErrorMessage(message);
                current.setCompletedAt(now());
                current.setLeaseUntil(null);
            });
        }
    }

    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    @PreDestroy
    public void close() { executor.shutdown(); }
}
