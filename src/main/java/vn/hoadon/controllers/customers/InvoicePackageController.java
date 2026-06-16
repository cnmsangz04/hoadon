package vn.hoadon.controllers.customers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseFilterDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseRequestDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageResponseDTO;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.InvoicePackageService;

import java.net.URI;
import java.util.List;
import java.util.Locale;
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
        boolean momoPending = isMomoPayment(result.getPaymentMethod()) && "PENDING".equals(result.getPaymentStatus());
        boolean vnpayPending = "VNPAY".equals(result.getPaymentMethod()) && "PENDING".equals(result.getPaymentStatus());
        return ResponseEntity.ok(Map.of(
                "message", momoPending
                        ? "Đã tạo giao dịch MoMo, vui lòng thanh toán trên MoMo"
                        : vnpayPending
                        ? "Đã tạo giao dịch VNPAY, vui lòng thanh toán trên VNPAY"
                        : "Thanh toán giả lập thành công",
                "purchase", result
        ));
    }

    @PostMapping("/purchases/{id}/retry-payment")
    public ResponseEntity<?> retryPayment(@PathVariable Long id) {
        InvoicePackagePurchaseDTO result = service.retryPayment(id, currentUser());
        boolean momoPending = isMomoPayment(result.getPaymentMethod()) && "PENDING".equals(result.getPaymentStatus());
        boolean vnpayPending = "VNPAY".equals(result.getPaymentMethod()) && "PENDING".equals(result.getPaymentStatus());
        return ResponseEntity.ok(Map.of(
                "message", momoPending
                        ? "Đã tạo lại giao dịch MoMo, vui lòng thanh toán trên MoMo"
                        : vnpayPending
                        ? "Đã tạo lại giao dịch VNPAY, vui lòng thanh toán trên VNPAY"
                        : "Đã xử lý thanh toán lại",
                "purchase", result
        ));
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<?> momoIpn(@RequestBody(required = false) Map<String, Object> payload) {
        InvoicePackagePurchaseDTO result = service.handleMomoIpn(payload != null ? payload : Map.of());
        return ResponseEntity.ok(Map.of(
                "message", "Đã nhận kết quả thanh toán MoMo",
                "purchase", result
        ));
    }

    @GetMapping("/momo/return")
    public ResponseEntity<Void> momoReturn(@RequestParam Map<String, String> params) {
        String redirectUrl = service.handleMomoReturn(params);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(redirectUrl).toString())
                .build();
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(service.handleVnpayIpn(params));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        String redirectUrl = service.handleVnpayReturn(params);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(redirectUrl).toString())
                .build();
    }

    @GetMapping("/my-purchases")
    public ResponseEntity<Page<InvoicePackagePurchaseDTO>> myPurchases(
            @ModelAttribute InvoicePackagePurchaseFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.listMyPurchases(user.getCompanyId(), filter, pageable));
    }

    @GetMapping("/purchases/{id}")
    public ResponseEntity<InvoicePackagePurchaseDTO> getMyPurchase(@PathVariable Long id) {
        return ResponseEntity.ok(service.getMyPurchase(id, currentUser()));
    }

    private boolean isMomoPayment(String paymentMethod) {
        return paymentMethod != null && paymentMethod.trim().toUpperCase(Locale.ROOT).startsWith("MOMO");
    }
}
