package vn.hoadon.controllers.customers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.dashboard.DashboardStatsDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.BuyInvoiceRepository;
import vn.hoadon.repositories.InvoiceRepository;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController extends BaseController {

    @Autowired
    private BuyInvoiceRepository buyInvoiceRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@AuthenticationPrincipal UserEntity user) {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Không xác định được công ty"));
        }

        Long companyId = user.getCompanyId();
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 1. Get buy_invoice data for company
        Optional<BuyInvoiceEntity> buyInvoiceOpt = buyInvoiceRepository
                .findFirstByCompanyIdAndStatusOrderByIdDesc(companyId, 1);

        if (buyInvoiceOpt.isPresent()) {
            BuyInvoiceEntity buyInvoice = buyInvoiceOpt.get();
            Integer amount = buyInvoice.getAmount() != null ? buyInvoice.getAmount() : 0;
            Integer amountUsed = buyInvoice.getAmountUsed() != null ? buyInvoice.getAmountUsed() : 0;

            stats.setTotalInvoices(amount);
            stats.setUsedInvoices(amountUsed);
            stats.setRemainingInvoices(amount - amountUsed);
        } else {
            stats.setTotalInvoices(0);
            stats.setUsedInvoices(0);
            stats.setRemainingInvoices(0);
        }

        // 2. Get issued invoices this year (status 3, 4, 5)
        // Get current year's start and end dates
        LocalDate startOfYear = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate endOfYear = LocalDate.of(LocalDate.now().getYear(), 12, 31);

        // Count invoices with status 3, 4, 5 in current year
        Long issuedCount = invoiceRepository.countByCompanyIdAndStatusInAndDateExportBetween(
                companyId,
                java.util.Arrays.asList((short) 3, (short) 4, (short) 5),
                startOfYear,
                endOfYear
        );
        stats.setIssuedThisYear(issuedCount != null ? issuedCount : 0L);

        // 3. Get total value of issued invoices this year
        Double totalValue = invoiceRepository.sumAmountByCompanyIdAndStatusInAndDateExportBetween(
                companyId,
                java.util.Arrays.asList((short) 3, (short) 4, (short) 5),
                startOfYear,
                endOfYear
        );
        stats.setValueThisYear(totalValue != null ? totalValue : 0.0);

        return ResponseEntity.ok(stats);
    }

    public static class ErrorDTO {
        public String message;
        public ErrorDTO(String m) {
            this.message = m;
        }
    }
}
