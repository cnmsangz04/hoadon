package vn.hoadon.controllers.customers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseRequestDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageResponseDTO;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.InvoicePackageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/invoice-packages")
public class InvoicePackageController extends BaseController {

    @Autowired
    private InvoicePackageService service;

    @GetMapping
    public ResponseEntity<List<InvoicePackageResponseDTO>> activePackages() {
        return ResponseEntity.ok(service.listActivePackages());
    }

    @PostMapping("/purchase")
    public ResponseEntity<?> purchase(@RequestBody InvoicePackagePurchaseRequestDTO dto) {
        UserEntity user = currentUser();
        InvoicePackagePurchaseDTO result = service.purchase(
                dto != null ? dto.getPackageId() : null,
                dto != null ? dto.getPaymentMethod() : null,
                user
        );
        return ResponseEntity.ok(Map.of(
                "message", "Thanh toán giả lập thành công",
                "purchase", result
        ));
    }

    @GetMapping("/my-purchases")
    public ResponseEntity<Page<InvoicePackagePurchaseDTO>> myPurchases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.listMyPurchases(user.getCompanyId(), pageable));
    }
}
