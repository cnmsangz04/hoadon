package vn.hoadon.invoice.template.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.invoice.template.persistence.InvoiceTemplateVersionRepository;
import vn.hoadon.invoice.template.storage.DocumentStorage;
import vn.hoadon.util.UploadPath;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class InvoiceTemplateStorageCleanupService {
    private final InvoiceTemplateVersionRepository versions;
    private final DocumentStorage storage;

    @PersistenceContext
    private EntityManager entityManager;

    public InvoiceTemplateStorageCleanupService(InvoiceTemplateVersionRepository versions,
                                                DocumentStorage storage) {
        this.versions = versions;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot(Long templateId, Long draftId) {
        List<Long> versionIds = versions.findByTemplateIdOrderByVersionNoDesc(templateId)
                .stream().map(version -> version.getId()).toList();
        List<Long> jobIds = new ArrayList<>();
        if (!versionIds.isEmpty() || draftId != null) {
            var query = entityManager.createQuery(
                    "select j.id from InvoiceRenderJobEntity j " +
                            "where (:hasVersions = false or j.templateVersionId in :versionIds) " +
                            "or (:draftId is not null and j.draftId = :draftId)", Long.class);
            query.setParameter("hasVersions", !versionIds.isEmpty());
            query.setParameter("versionIds", versionIds.isEmpty() ? List.of(-1L) : versionIds);
            query.setParameter("draftId", draftId);
            jobIds.addAll(query.getResultList());
        }

        List<String> artifactKeys = new ArrayList<>();
        List<Long> artifactIds = new ArrayList<>();
        if (!jobIds.isEmpty() || !versionIds.isEmpty()) {
            var query = entityManager.createQuery(
                    "select a.id, a.storageKey from InvoiceRenderArtifactEntity a " +
                            "where (:hasJobs = false or a.jobId in :jobIds) " +
                            "or (:hasVersions = false or a.templateVersionId in :versionIds)");
            query.setParameter("hasJobs", !jobIds.isEmpty());
            query.setParameter("jobIds", jobIds.isEmpty() ? List.of(-1L) : jobIds);
            query.setParameter("hasVersions", !versionIds.isEmpty());
            query.setParameter("versionIds", versionIds.isEmpty() ? List.of(-1L) : versionIds);
            for (Object[] row : (List<Object[]>) query.getResultList()) {
                artifactIds.add((Long) row[0]);
                artifactKeys.add((String) row[1]);
            }
        }

        Set<Long> assetIds = new LinkedHashSet<>();
        if (draftId != null) {
            assetIds.addAll(entityManager.createQuery(
                            "select p.assetId from InvoiceAssetPinEntity p where p.ownerType = 'DRAFT' and p.ownerId = :owner", Long.class)
                    .setParameter("owner", draftId).getResultList());
        }
        if (!versionIds.isEmpty()) {
            assetIds.addAll(entityManager.createQuery(
                            "select distinct a.assetId from InvoiceTemplateVersionAssetEntity a where a.templateVersionId in :ids", Long.class)
                    .setParameter("ids", versionIds).getResultList());
            assetIds.addAll(entityManager.createQuery(
                            "select p.assetId from InvoiceAssetPinEntity p where p.ownerType = 'VERSION' and p.ownerId in :ids", Long.class)
                    .setParameter("ids", versionIds).getResultList());
        }

        List<String> assetKeys = new ArrayList<>();
        if (!assetIds.isEmpty()) {
            var query = entityManager.createQuery(
                    "select a.id, a.storageKey from InvoiceAssetEntity a where a.id in :ids");
            query.setParameter("ids", assetIds);
            for (Object[] row : (List<Object[]>) query.getResultList()) assetKeys.add((String) row[1]);
        }
        return new Snapshot(versionIds, jobIds, artifactIds, assetIds, artifactKeys, assetKeys);
    }

    @Transactional
    public void removeDatabaseRows(Snapshot snapshot) {
        if (!snapshot.artifactIds().isEmpty()) {
            entityManager.createQuery("delete from InvoiceRenderArtifactEntity a where a.id in :ids")
                    .setParameter("ids", snapshot.artifactIds()).executeUpdate();
        }
        if (!snapshot.jobIds().isEmpty()) {
            entityManager.createQuery("delete from InvoiceRenderJobEntity j where j.id in :ids")
                    .setParameter("ids", snapshot.jobIds()).executeUpdate();
        }
        if (!snapshot.assetIds().isEmpty()) {
            entityManager.createNativeQuery("delete from invoice_assets where id in (:ids) " +
                            "and not exists (select 1 from invoice_asset_pins p where p.asset_id = invoice_assets.id) " +
                            "and not exists (select 1 from invoice_template_version_assets va where va.asset_id = invoice_assets.id) " +
                            "and not exists (select 1 from invoice_template_import_jobs ij where ij.source_asset_id = invoice_assets.id)")
                    .setParameter("ids", snapshot.assetIds()).executeUpdate();
        }
        entityManager.flush();
    }

    @Transactional(readOnly = true)
    public void deleteFiles(Snapshot snapshot) {
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(snapshot.artifactKeys());
        if (!snapshot.assetKeys().isEmpty()) {
            var retained = new LinkedHashSet<>(entityManager.createQuery(
                            "select a.storageKey from InvoiceAssetEntity a where a.storageKey in :keys", String.class)
                    .setParameter("keys", snapshot.assetKeys()).getResultList());
            snapshot.assetKeys().stream().filter(key -> !retained.contains(key)).forEach(keys::add);
        }
        for (String key : keys) {
            try {
                storage.delete(key);
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Transactional
    public void deleteDraftPreviewJobs(Long draftId) {
        if (draftId == null) return;
        List<Long> jobIds = entityManager.createQuery(
                        "select j.id from InvoiceRenderJobEntity j where j.draftId = :draftId and j.purpose = 'DRAFT_PREVIEW' and j.state in ('SUCCEEDED', 'FAILED')",
                        Long.class)
                .setParameter("draftId", draftId)
                .getResultList();
        deleteRenderJobs(jobIds);
    }

    @Transactional
    public void deleteCompletedTemplateSamples(String scope, Long companyId, Long templateVersionId) {
        if (scope == null || templateVersionId == null) return;
        List<Long> jobIds = entityManager.createQuery(
                        "select j.id from InvoiceRenderJobEntity j " +
                                "where j.purpose = 'TEMPLATE_SAMPLE' " +
                                "and j.templateVersionId = :versionId " +
                                "and j.scope = :scope " +
                                "and ((:companyId is null and j.companyId is null) or j.companyId = :companyId) " +
                                "and j.state in ('SUCCEEDED', 'FAILED')",
                        Long.class)
                .setParameter("versionId", templateVersionId)
                .setParameter("scope", scope)
                .setParameter("companyId", companyId)
                .getResultList();
        deleteRenderJobs(jobIds);
    }

    @Transactional
    public void deleteRenderJobs(List<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) return;
        List<Long> artifactIds = new ArrayList<>();
        List<String> artifactKeys = new ArrayList<>();
        var query = entityManager.createQuery(
                "select a.id, a.storageKey from InvoiceRenderArtifactEntity a where a.jobId in :ids");
        query.setParameter("ids", jobIds);
        for (Object[] row : (List<Object[]>) query.getResultList()) {
            artifactIds.add((Long) row[0]);
            artifactKeys.add((String) row[1]);
        }
        entityManager.createQuery("delete from InvoiceAssetPinEntity p where p.ownerType = 'RENDER_JOB' and p.ownerId in :ids")
                .setParameter("ids", jobIds).executeUpdate();
        if (!artifactIds.isEmpty()) {
            entityManager.createQuery("delete from InvoiceRenderArtifactEntity a where a.id in :ids")
                    .setParameter("ids", artifactIds).executeUpdate();
        }
        entityManager.createQuery("delete from InvoiceRenderJobEntity j where j.id in :ids")
                .setParameter("ids", jobIds).executeUpdate();
        entityManager.flush();
        for (String key : artifactKeys) {
            try {
                storage.delete(key);
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Scheduled(
            fixedDelayString = "${invoice.template.sample-cleanup-delay-ms:3600000}",
            initialDelayString = "${invoice.template.sample-cleanup-initial-delay-ms:10000}"
    )
    @Transactional
    public void cleanupExpiredSampleJobs() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10);
        List<Long> jobIds = entityManager.createQuery(
                        "select j.id from InvoiceRenderJobEntity j " +
                                "where j.purpose = 'TEMPLATE_SAMPLE' " +
                                "and j.state in ('SUCCEEDED', 'FAILED') " +
                                "and j.createdAt < :cutoff", Long.class)
                .setParameter("cutoff", cutoff)
                .getResultList();
        deleteRenderJobs(jobIds);
        cleanupOrphanAssetRows();
        cleanupOrphanAssetFiles();
    }

    private void cleanupOrphanAssetRows() {
        entityManager.createNativeQuery(
                        "delete from invoice_asset_pins " +
                                "where (owner_type = 'RENDER_JOB' and not exists (select 1 from invoice_render_jobs where id = owner_id)) " +
                                "or (owner_type = 'VERSION' and not exists (select 1 from invoice_template_versions where id = owner_id)) " +
                                "or (owner_type = 'DRAFT' and not exists (select 1 from invoice_template_drafts where id = owner_id)) " +
                                "or (owner_type = 'SNAPSHOT' and not exists (select 1 from invoice_document_snapshots where id = owner_id))")
                .executeUpdate();
        List<Object[]> orphanAssets = entityManager.createQuery(
                        "select a.id, a.storageKey from InvoiceAssetEntity a " +
                                "where not exists (select p.id from InvoiceAssetPinEntity p where p.assetId = a.id) " +
                                "and not exists (select v.id from InvoiceTemplateVersionAssetEntity v where v.assetId = a.id) " +
                                "and not exists (select i.id from InvoiceTemplateImportJobEntity i where i.sourceAssetId = a.id)", Object[].class)
                .getResultList();
        if (orphanAssets.isEmpty()) return;
        List<Long> ids = orphanAssets.stream().map(row -> (Long) row[0]).toList();
        entityManager.createQuery("delete from InvoiceAssetEntity a where a.id in :ids")
                .setParameter("ids", ids)
                .executeUpdate();
        entityManager.flush();
        for (Object[] row : orphanAssets) {
            try {
                storage.delete((String) row[1]);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void cleanupOrphanAssetFiles() {
        Set<String> referencedKeys = new HashSet<>(entityManager.createQuery(
                "select a.storageKey from InvoiceAssetEntity a", String.class).getResultList());
        Path root = UploadPath.basePath();
        if (!Files.isDirectory(root)) return;
        try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                Path relative = root.relativize(path);
                if (relative.getNameCount() != 3 ||
                        !"invoice-assets".equals(relative.getName(1).toString())) return;
                String key = relative.toString().replace(path.getFileSystem().getSeparator(), "/");
                if (referencedKeys.contains(key)) return;
                try {
                    storage.delete(key);
                } catch (RuntimeException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    public void deletePhoto(String photo) {
        try {
            UploadPath.deletePublicUrl(photo);
        } catch (IOException ignored) {
        }
    }

    public record Snapshot(List<Long> versionIds, List<Long> jobIds, List<Long> artifactIds,
                           Set<Long> assetIds, List<String> artifactKeys, List<String> assetKeys) { }
}
