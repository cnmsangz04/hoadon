package vn.hoadon.controllers.customers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.services.TaxCodeLookupService;

import java.util.Map;

@RestController
@RequestMapping("/v1/tax-code-lookup")
public class TaxCodeLookupController extends BaseController {

    private final TaxCodeLookupService taxCodeLookupService;

    public TaxCodeLookupController(TaxCodeLookupService taxCodeLookupService) {
        this.taxCodeLookupService = taxCodeLookupService;
    }

    @GetMapping("/{taxCode}")
    public ResponseEntity<?> lookup(@PathVariable String taxCode) {
        permission("invoice-save");
        try {
            return ResponseEntity.ok(taxCodeLookupService.lookup(taxCode));
        } catch (TaxCodeLookupService.LookupException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatus());
            return ResponseEntity.status(status != null ? status : HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", ex.getMessage()));
        }
    }
}
