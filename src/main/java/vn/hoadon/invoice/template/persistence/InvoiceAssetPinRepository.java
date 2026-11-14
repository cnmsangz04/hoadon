package vn.hoadon.invoice.template.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceAssetPinRepository extends JpaRepository<InvoiceAssetPinEntity, Long> {
    boolean existsByAssetIdAndOwnerTypeAndOwnerId(Long assetId, String ownerType, Long ownerId);
}
