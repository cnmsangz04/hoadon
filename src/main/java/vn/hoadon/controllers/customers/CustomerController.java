package vn.hoadon.controllers.customers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.customer.CustomerFilterDTO;
import vn.hoadon.entity.CustomersEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.CustomerService;

@RestController
@RequestMapping("/v1/categories/customer")
public class CustomerController extends BaseController {

	@Autowired
	private CustomerService customerService;

	@PostMapping("/list")
	public Map<String, Object> list(@RequestBody CustomerFilterDTO filterDTO,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		permission("category-customer-list");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof UserEntity) {
			UserEntity user = (UserEntity) auth.getPrincipal();
			filterDTO.setCompanyId(user.getCompanyId());
		}
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("status"), Sort.Order.asc("code")));
		Page<CustomersEntity> resultPage = customerService.list(filterDTO, pageable);
		return toPaginationResponse(resultPage);
	}

	private Map<String, Object> toPaginationResponse(Page<CustomersEntity> p) {
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
	public ResponseEntity<?> save(@RequestBody CustomersEntity customer) {
		try {
			permission("category-customer-save");
			if (customer == null) {
				return ResponseEntity.badRequest().body(Map.of("message", "Thiếu thông tin khách hàng"));
			}
			if (!hasText(customer.getCompanyName()) && !hasText(customer.getBuyerName())) {
				return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập Đơn vị mua hàng hoặc Người mua hàng"));
			}
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.getPrincipal() instanceof UserEntity) {
				UserEntity user = (UserEntity) auth.getPrincipal();

				customer.setCompanyId(user.getCompanyId());
			} else {
				return ResponseEntity.status(401).body("Hết phiên làm việc");
			}
			CustomersEntity savedCustomer = customerService.saveOrUpdate(customer);
			return ResponseEntity.ok(savedCustomer);

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
		}

	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable("id") Long id) {
		try {
			permission("category-customer-save");
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !(auth.getPrincipal() instanceof UserEntity)) {
				return ResponseEntity.status(401).body(Map.of("message", "Hết phiên làm việc"));
			}
			UserEntity user = (UserEntity) auth.getPrincipal();
			customerService.delete(id, user.getCompanyId());
			return ResponseEntity.ok(Map.of("message", "Xóa khách hàng thành công"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "Xóa khách hàng thất bại: " + e.getMessage()));
		}
	}

	@PostMapping("/get")
	public ResponseEntity<?> get(@RequestBody Map<String, Object> payload) {
		permission("category-customer-list");
		String code = null;
		if (payload != null && payload.get("code") != null) {
			code = String.valueOf(payload.get("code")).trim();
		}
		if (code == null || code.isEmpty()) {
			return ResponseEntity.badRequest().body("Thiếu mã khách hàng");
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof UserEntity)) {
			return ResponseEntity.status(401).body("Hết phiên làm việc");
		}
		UserEntity user = (UserEntity) auth.getPrincipal();

		CustomerFilterDTO filterDTO = new CustomerFilterDTO();
		filterDTO.setCompanyId(user.getCompanyId());
		// Avoid calling setCode since DTO may not have this method; fetch a small page then filter
		Pageable pageable = PageRequest.of(0, 200, Sort.by(Sort.Order.asc("code")));
		Page<CustomersEntity> page = customerService.list(filterDTO, pageable);
		if (page.hasContent()) {
			for (CustomersEntity c : page.getContent()) {
				if (code.equalsIgnoreCase(String.valueOf(c.getCode()))) {
					return ResponseEntity.ok(c);
				}
			}
		}
		return ResponseEntity.status(404).body("Không tìm thấy khách hàng");
	}
}
