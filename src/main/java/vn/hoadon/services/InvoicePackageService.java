package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.dto.invoicepackage.InvoicePackageFilterDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseFilterDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageRequestDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageResponseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageStatisticsDTO;
import vn.hoadon.entity.UserEntity;

import java.util.List;

public interface InvoicePackageService {
    Page<InvoicePackageResponseDTO> listPackages(InvoicePackageFilterDTO filter, Pageable pageable);

    List<InvoicePackageResponseDTO> listActivePackages();

    InvoicePackageResponseDTO savePackage(InvoicePackageRequestDTO dto);

    void deletePackage(Long id);

    InvoicePackagePurchaseDTO purchase(Long packageId, String paymentMethod, UserEntity user);

    Page<InvoicePackagePurchaseDTO> listPurchases(InvoicePackagePurchaseFilterDTO filter, Pageable pageable);

    Page<InvoicePackagePurchaseDTO> listMyPurchases(Long companyId, Pageable pageable);

    InvoicePackageStatisticsDTO statistics(InvoicePackagePurchaseFilterDTO filter);

    byte[] exportPurchases(InvoicePackagePurchaseFilterDTO filter);
}
