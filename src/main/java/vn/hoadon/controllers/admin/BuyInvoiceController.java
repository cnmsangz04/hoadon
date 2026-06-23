package vn.hoadon.controllers.admin;

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.buyinvoice.BuyInvoiceCreateDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceFilterDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceHistoryDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceHistoryFilterDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceListItemDTO;
import vn.hoadon.dto.common.IdRequestDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.security.UserRoles;
import vn.hoadon.services.BuyInvoiceHistoryService;
import vn.hoadon.services.BuyInvoiceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("v1/administrator/buy-invoice")
public class BuyInvoiceController extends BaseController {

    @Autowired
    private BuyInvoiceService service;

    @Autowired
    private BuyInvoiceHistoryService historyService;

    @PostMapping("/list")
    public ResponseEntity<Page<BuyInvoiceListItemDTO>> list(
            @RequestBody(required = false) BuyInvoiceFilterDTO filter,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        permission("buy-invoice-list");
        
        if (filter == null) filter = new BuyInvoiceFilterDTO();
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 25;

        // Khu quản trị hệ thống xem toàn hệ thống; user thường chỉ xem công ty của mình.
        UserEntity user = currentUser();
        Long actorCompanyId = user != null ? user.getCompanyId() : null;
        Integer actorRole = user != null ? user.getRole() : null;
        if (!UserRoles.canAccessAdminArea(actorRole)) {
            filter.setCompanyId(actorCompanyId);
        }

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createdAt").descending());

        return ResponseEntity.ok(service.list(filter, pageable));
    }

    @PostMapping("/histories")
    public ResponseEntity<Page<BuyInvoiceHistoryDTO>> histories(
            @RequestBody(required = false) BuyInvoiceHistoryFilterDTO filter,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        permission("buy-invoice-list");

        if (filter == null) filter = new BuyInvoiceHistoryFilterDTO();
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 10;

        UserEntity user = currentUser();
        Integer actorRole = user != null ? user.getRole() : null;
        if (!UserRoles.canAccessAdminArea(actorRole)) {
            filter.setCompanyId(user != null ? user.getCompanyId() : null);
        }

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createdAt").descending());
        return ResponseEntity.ok(historyService.list(filter, pageable));
    }

    @PostMapping("/get")
    public ResponseEntity<BuyInvoiceEntity> get(@RequestBody @Valid IdRequestDTO request) {
        permission("buy-invoice-list");
        return ResponseEntity.of(service.findById(request.getId()));
    }

    // Sử dụng DTO để nhận dữ liệu
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody BuyInvoiceCreateDTO dto) {
        permission("buy-invoice-save");
        UserEntity user = currentUser();
        
        // For admin page: use companyId from DTO (selected from form)
        // Admin can manage buy_invoice for any company
        Long companyId = dto.getCompanyId();
        
        if (companyId == null) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Vui lòng chọn công ty"));
        }
        
        BuyInvoiceEntity entity;
        BuyInvoiceEntity before = null;
        String changeType;
        
        // Nếu cập nhật (id != null), nạp entity hiện có để giữ amountUsed
        if (dto.getId() != null) {
            entity = service.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi buy_invoice với id: " + dto.getId()));
            before = snapshot(entity);
            changeType = "ADMIN_UPDATE";
            
            // Validate: amount cannot be less than amountUsed
            Integer amountUsed = entity.getAmountUsed() != null ? entity.getAmountUsed() : 0;
            if (dto.getAmount() != null && dto.getAmount() < amountUsed) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("message", "Số lượng không được nhỏ hơn số lượng đã sử dụng (" + amountUsed + " hóa đơn đã cấp số)"));
            }
            
            // Chỉ cập nhật các trường người dùng được phép sửa
            entity.setAmount(dto.getAmount());
            entity.setStatus(dto.getStatus());
            // Keep amountUsed unchanged
        } else {
            // Creating new entity - CHECK IF COMPANY ALREADY HAS ONE
            // Quy tắc nghiệp vụ: mỗi công ty chỉ có một bản ghi buy_invoice
            java.util.List<BuyInvoiceEntity> existingInvoices = service.findAll((root, query, cb) -> 
                cb.equal(root.get("companyId"), companyId)
            );
            
            if (!existingInvoices.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("message", "Công ty đã có gói hóa đơn. Vui lòng cập nhật gói hiện tại thay vì tạo mới"));
            }
            
            entity = new BuyInvoiceEntity();
            entity.setCompanyId(companyId);
            entity.setAmount(dto.getAmount());
            entity.setAmountUsed(0); // Initialize to 0 for new records
            entity.setStatus(dto.getStatus());
            changeType = "ADMIN_CREATE";
        }
        
        BuyInvoiceEntity saved = service.saveOrUpdate(entity);
        historyService.record(
                before,
                snapshot(saved),
                changeType,
                "ADMIN",
                user,
                null,
                null,
                null,
                noteOrDefault(dto.getNote(), changeType.equals("ADMIN_CREATE") ? "Admin thêm hạn mức hóa đơn" : "Admin cập nhật hạn mức hóa đơn")
        );
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody @Valid IdRequestDTO request) {
        permission("buy-invoice-delete");
        // Check if buy_invoice exists
        BuyInvoiceEntity entity = service.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi buy_invoice với id: " + request.getId()));
        
        // Check if status = 1 (active)
        Integer status = entity.getStatus() != null ? entity.getStatus() : 0;
        if (status == 1) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Không thể xóa gói hóa đơn đang kích hoạt. Vui lòng ngưng kích hoạt trước khi xóa"));
        }
        
        // Check if amount_used > 0
        Integer amountUsed = entity.getAmountUsed() != null ? entity.getAmountUsed() : 0;
        if (amountUsed > 0) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Không thể xóa gói hóa đơn đã được sử dụng (" + amountUsed + " hóa đơn đã cấp số)"));
        }
        
        historyService.record(
                snapshot(entity),
                null,
                "ADMIN_DELETE",
                "ADMIN",
                currentUser(),
                null,
                null,
                null,
                "Admin xóa hạn mức hóa đơn"
        );
        service.delete(request.getId());
        return ResponseEntity.noContent().build();
    }

    private BuyInvoiceEntity snapshot(BuyInvoiceEntity source) {
        if (source == null) return null;
        BuyInvoiceEntity copy = new BuyInvoiceEntity();
        copy.setId(source.getId());
        copy.setCompanyId(source.getCompanyId());
        copy.setAmount(source.getAmount());
        copy.setAmountUsed(source.getAmountUsed());
        copy.setStatus(source.getStatus());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private String noteOrDefault(String note, String defaultValue) {
        return note != null && !note.isBlank() ? note.trim() : defaultValue;
    }
}
