package vn.hoadon.controllers.admin;

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.buyinvoice.BuyInvoiceCreateDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceFilterDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceListItemDTO;
import vn.hoadon.dto.common.IdRequestDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
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

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createdAt").descending());

        return ResponseEntity.ok(service.list(filter, pageable));
    }

    @PostMapping("/get")
    public ResponseEntity<BuyInvoiceEntity> get(@RequestBody @Valid IdRequestDTO request) {
        return ResponseEntity.of(service.findById(request.getId()));
    }

    // Sử dụng DTO để nhận dữ liệu
    @PostMapping("/create")
    public ResponseEntity<BuyInvoiceEntity> create(@RequestBody BuyInvoiceCreateDTO dto) {
        BuyInvoiceEntity entity = new BuyInvoiceEntity();
        entity.setId(dto.getId());
        entity.setCompanyId(dto.getCompanyId());
        entity.setAmount(dto.getAmount());
        entity.setStatus(dto.getStatus());
        return ResponseEntity.ok(service.saveOrUpdate(entity));
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody @Valid IdRequestDTO request) {
        service.delete(request.getId());
        return ResponseEntity.noContent().build();
    }
}