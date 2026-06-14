package vn.hoadon.controllers.admin;

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.buyinvoice.BuyInvoiceCreateDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceFilterDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceListItemDTO;
import vn.hoadon.dto.common.IdRequestDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.entity.UserEntity;
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

        // Derive companyId from current user for non-root users
        UserEntity user = currentUser();
        Long actorCompanyId = user != null ? user.getCompanyId() : null;
        Integer actorRole = user != null ? user.getRole() : null;
        boolean isRoot = actorRole != null && actorRole == 0;
        if (!isRoot) {
            filter.setCompanyId(actorCompanyId);
        }

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createdAt").descending());

        return ResponseEntity.ok(service.list(filter, pageable));
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
        
        // If updating (id != null), load existing entity to preserve amountUsed
        if (dto.getId() != null) {
            entity = service.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi buy_invoice với id: " + dto.getId()));
            
            // Validate: amount cannot be less than amountUsed
            Integer amountUsed = entity.getAmountUsed() != null ? entity.getAmountUsed() : 0;
            if (dto.getAmount() != null && dto.getAmount() < amountUsed) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("message", "Số lượng không được nhỏ hơn số lượng đã sử dụng (" + amountUsed + " hóa đơn đã cấp số)"));
            }
            
            // Update only the fields that user can modify
            entity.setAmount(dto.getAmount());
            entity.setStatus(dto.getStatus());
            // Keep amountUsed unchanged
        } else {
            // Creating new entity - CHECK IF COMPANY ALREADY HAS ONE
            // Business rule: Each company can only have ONE buy_invoice record
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
        }
        
        return ResponseEntity.ok(service.saveOrUpdate(entity));
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
        
        service.delete(request.getId());
        return ResponseEntity.noContent().build();
    }
}