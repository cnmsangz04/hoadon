package vn.hoadon.controller.admin;

import vn.hoadon.dto.buyinvoice.BuyInvoiceDeleteDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceFilterDTO;
import vn.hoadon.dto.buyinvoice.BuyInvoiceGetDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.services.BuyInvoiceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Optional;

@RestController
@RequestMapping("v1/administrator/buy-invoice")
public class BuyInvoiceController {

    @Autowired
    private BuyInvoiceService service;

    @PostMapping("/list")
    public ResponseEntity<Page<BuyInvoiceEntity>> list(
            @RequestBody BuyInvoiceFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(service.list(filter, pageable));
    }

    @PostMapping("/get")
    public ResponseEntity<BuyInvoiceEntity> get(
            @RequestBody @Valid BuyInvoiceGetDTO request
    ) {
        return ResponseEntity.of(service.findById(request.getId()));
    }

    @PostMapping("/create")
    public ResponseEntity<BuyInvoiceEntity> create(
            @RequestBody BuyInvoiceEntity entity
    ) {
        return ResponseEntity.ok(service.saveOrUpdate(entity));
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> delete(
            @RequestBody @Valid BuyInvoiceDeleteDTO request
    ) {
        service.delete(request.getId());
        return ResponseEntity.noContent().build();
    }
}
