package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.bank.BankFilterDTO;
import vn.hoadon.entity.BankEntity;
import vn.hoadon.services.BankService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/administrator/bank")
public class BankController extends BaseController {

    @Autowired
    private BankService bankService;

    @PostMapping("/list")
    public Map<String, Object> list(
            @RequestBody BankFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                    Sort.Order.desc("status"),
                    Sort.Order.asc("abbreviation"),
                    Sort.Order.asc("name")
                )
        );

        Page<BankEntity> resultPage = bankService.list(filter, pageable);
        
        return toPaginationResponse(resultPage);
    }


    private Map<String, Object> toPaginationResponse(Page<BankEntity> p) {
        Map<String, Object> res = new HashMap<>();
        long total = p.getTotalElements();
        int size = p.getSize();
        int currentPage = p.getNumber() + 1;
        int lastPage = Math.max(1, p.getTotalPages());
        int numberOfElements = p.getNumberOfElements();
        
        long from = total == 0 ? 0 : ((long) (currentPage - 1) * size) + 1;
        long to = total == 0 ? 0 : (from + numberOfElements - 1);

        res.put("data", p.getContent());
        res.put("total", total);
        res.put("per_page", size);
        res.put("current_page", currentPage);
        res.put("last_page", lastPage);
        res.put("from", from);
        res.put("to", to);
        
        res.put("prev_page_url", currentPage > 1 ? currentPage - 1 : null);
        res.put("next_page_url", currentPage < lastPage ? currentPage + 1 : null);
        
        return res;
    }

    @GetMapping("/all")
    public Map<String, Object> listAll(@RequestParam(required = false) Integer status) {
        Map<String, Object> res = new HashMap<>();
        res.put("data", bankService.list(status));
        return res;
    }

    @GetMapping("/{abbreviation}")
    public BankEntity getByAbbreviation(@PathVariable String abbreviation) {
        return bankService.findByAbbreviation(abbreviation).orElse(null);
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody BankEntity bank) {
        try {
			/*
			 * if (bank.getId() == null &&
			 * bankService.findByAbbreviation(bank.getAbbreviation()).isPresent()) { return
			 * ResponseEntity.badRequest().body("Mã ngân hàng này đã tồn tại!"); }
			 */
            
            BankEntity savedBank = bankService.saveOrUpdate(bank);
            return ResponseEntity.ok(savedBank);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}