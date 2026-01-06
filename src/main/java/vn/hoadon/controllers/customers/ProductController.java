package vn.hoadon.controllers.customers;

import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.product.ProductFilterDTO;
import vn.hoadon.entity.ProductsEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.VatRatesEntity;
import vn.hoadon.services.ProductService;
import vn.hoadon.services.VatRatesService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/v1/categories/product")
public class ProductController extends BaseController {

	@Autowired
	private ProductService productService;

    @Autowired
    private VatRatesService vatRatesService;

	@PostMapping("/list")
	public Map<String, Object> list(@RequestBody ProductFilterDTO filterDTO, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserEntity) {
            UserEntity user = (UserEntity) auth.getPrincipal();
            // Gán companyId vào filter để Service lọc dữ liệu
            filterDTO.setCompanyId(user.getCompanyId());
        }
		
		
		Pageable pageable = PageRequest.of(page, size,
				Sort.by(Sort.Order.desc("status"), Sort.Order.asc("code"), Sort.Order.asc("name"))
		);
		Page<ProductsEntity> resultPage = productService.list(filterDTO, pageable);
		return toPaginationResponse(resultPage);
	}

	private Map<String, Object> toPaginationResponse(Page<ProductsEntity> p) {
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
	
	@PostMapping("/save")
	public ResponseEntity<?> save(@RequestBody ProductsEntity product) {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	        if (auth != null && auth.getPrincipal() instanceof UserEntity) {
	            UserEntity user = (UserEntity) auth.getPrincipal();
	            // Gán companyId vào filter để Service lọc dữ liệu
	            product.setCompanyId(user.getCompanyId());
	        }
	        
			ProductsEntity savedProduct = productService.saveOrUpdate(product);
			return ResponseEntity.ok(savedProduct);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
		}
	}

    // Provide VAT rates (code and label) for current user, only active ones
    @GetMapping("/vat-rates")
    public List<Map<String, Object>> getVatRates() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = null;
        if (auth != null && auth.getPrincipal() instanceof UserEntity) {
            userId = Math.toIntExact(((UserEntity) auth.getPrincipal()).getId());
        }
        // Default pagination: up to 100 items, sort by prioritize asc
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "prioritize"));
        Page<VatRatesEntity> page = vatRatesService.pageByUser(userId, 1, pageable, null);
        return page.getContent().stream().map(it -> {
            Map<String, Object> m = new HashMap<>();
            m.put("code", it.getCode());
            m.put("label", it.getLabel());
            return m;
        }).collect(Collectors.toList());
    }
	
}