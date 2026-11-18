package vn.hoadon.invoice.template.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.invoice.template.persistence.InvoiceTemplateVersionEntity;
import vn.hoadon.invoice.template.persistence.InvoiceTemplateVersionRepository;

import java.util.List;

@Service
@Transactional
public class InvoiceTemplateHistoryService {
    private static final int HISTORY_LIMIT = 3;

    private final InvoiceTemplateVersionRepository versions;
    private final EntityManager entityManager;

    public InvoiceTemplateHistoryService(InvoiceTemplateVersionRepository versions, EntityManager entityManager) {
        this.versions = versions;
        this.entityManager = entityManager;
    }

    public void trim(Long templateId) {
        var history = versions.findByTemplateIdOrderByVersionNoDesc(templateId);
        if (history.size() <= HISTORY_LIMIT) return;
        remove(history.subList(HISTORY_LIMIT, history.size()), history.getFirst().getId());
    }

    public void delete(Long templateId, Long versionId) {
        var history = versions.findByTemplateIdOrderByVersionNoDesc(templateId);
        var target = history.stream().filter(version -> version.getId().equals(versionId))
                .findFirst().orElseThrow(TemplateException::missing);
        if (history.getFirst().getId().equals(versionId)) {
            throw TemplateException.conflict("Không thể xóa bản đang áp dụng");
        }
        remove(List.of(target), history.getFirst().getId());
    }

    public void deleteAll(Long templateId) {
        var history = versions.findByTemplateIdOrderByVersionNoDesc(templateId);
        if (!history.isEmpty()) remove(history, null);
    }

    private void remove(List<InvoiceTemplateVersionEntity> removed, Long latestId) {
        var ids = removed.stream().map(InvoiceTemplateVersionEntity::getId).toList();
        entityManager.flush();

        entityManager.createQuery("update FormInvoiceEntity f set f.templateVersionId = :latest where f.templateVersionId in :ids")
                .setParameter("latest", latestId).setParameter("ids", ids).executeUpdate();
        entityManager.createQuery("update InvoiceTemplateEntity t set t.sourceVersionId = null where t.sourceVersionId in :ids")
                .setParameter("ids", ids).executeUpdate();
        var drafts = entityManager.createQuery(
                        "select d from InvoiceTemplateDraftEntity d where d.baseVersionId in :ids",
                        vn.hoadon.invoice.template.persistence.InvoiceTemplateDraftEntity.class)
                .setParameter("ids", ids).getResultList();
        drafts.forEach(draft -> draft.setBaseVersionId(null));

        entityManager.createQuery("update InvoiceRenderJobEntity j set j.templateVersionId = null where j.templateVersionId in :ids")
                .setParameter("ids", ids).executeUpdate();
        entityManager.createNativeQuery("update invoice_render_artifacts set template_version_id = null where template_version_id in (:ids)")
                .setParameter("ids", ids).executeUpdate();

        var snapshots = entityManager.createQuery("select s.id from InvoiceDocumentSnapshotEntity s where s.templateVersionId in :ids", Long.class)
                .setParameter("ids", ids).getResultList();
        if (!snapshots.isEmpty()) {
            entityManager.createQuery("update InvoiceRenderJobEntity j set j.snapshotId = null where j.snapshotId in :ids")
                    .setParameter("ids", snapshots).executeUpdate();
            entityManager.createNativeQuery("update invoice_render_artifacts set snapshot_id = null where snapshot_id in (:ids)")
                    .setParameter("ids", snapshots).executeUpdate();
            entityManager.createQuery("delete from InvoiceAssetPinEntity p where p.ownerType = 'SNAPSHOT' and p.ownerId in :ids")
                    .setParameter("ids", snapshots).executeUpdate();
            entityManager.createQuery("delete from InvoiceDocumentSnapshotEntity s where s.id in :ids")
                    .setParameter("ids", snapshots).executeUpdate();
        }
        entityManager.createQuery("delete from InvoiceTemplateVersionAssetEntity a where a.templateVersionId in :ids")
                .setParameter("ids", ids).executeUpdate();
        entityManager.createQuery("delete from InvoiceAssetPinEntity p where p.ownerType = 'VERSION' and p.ownerId in :ids")
                .setParameter("ids", ids).executeUpdate();
        entityManager.flush();
        removed.forEach(entityManager::remove);
        entityManager.flush();
    }
}
