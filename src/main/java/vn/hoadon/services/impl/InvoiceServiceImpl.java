package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.hoadon.dto.InvoiceDTO;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.InvoiceService;

import java.util.HashMap;
import java.util.Map;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private FormInvoiceRepository formInvoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Page<InvoiceDTO> search(String q, Short status, Pageable pageable) {
        Page<InvoiceEntity> page = invoiceRepository.search(q, status, pageable);
        // Preload related forms and users to avoid N+1 lookups
        Map<Long, FormInvoiceEntity> formCache = new HashMap<>();
        Map<Long, UserEntity> userCache = new HashMap<>();

        return page.map(inv -> {
            InvoiceDTO dto = new InvoiceDTO();
            dto.id = inv.getId();
            dto.no = inv.getNo();
            dto.dateExport = inv.getDateExport();
            dto.lookupCode = inv.getLookupCode();
            dto.codeCqt = inv.getCodeCqt();
            dto.amount = inv.getAmount();
            dto.status = inv.getStatus();
            Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
            if (formId != null) {
                FormInvoiceEntity form = formCache.computeIfAbsent(formId, id -> formInvoiceRepository.findById(id).orElse(null));
                if (form != null) {
                    dto.formCode = form.getFormCode();
                    dto.serial = form.getSerial();
                }
            }
            dto.customerName = null; // customer info is stored as JSON/text in invoices.customer; parse if needed
            Long userId = inv.getUserId() != null ? inv.getUserId().longValue() : null;
            dto.userId = userId;
            if (userId != null) {
                UserEntity u = userCache.computeIfAbsent(userId, id -> userRepository.findById(id).orElse(null));
                dto.username = (u != null ? u.getUsername() : null);
            }
            return dto;
        });
    }
}
