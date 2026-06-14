package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceNumberEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceNumberRepository;
import vn.hoadon.services.InvoiceNumberService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceNumberServiceImpl implements InvoiceNumberService {

    private final InvoiceNumberRepository repository;
    private final FormInvoiceRepository formInvoiceRepository;

    @Autowired
    public InvoiceNumberServiceImpl(InvoiceNumberRepository repository, FormInvoiceRepository formInvoiceRepository) {
        this.repository = repository;
        this.formInvoiceRepository = formInvoiceRepository;
    }

    @Override
    public InvoiceNumberEntity create(InvoiceNumberEntity entity) {
        // Suy ra category từ form liên kết nếu chưa truyền vào
        if (entity.getCategory() == null) {
            Long formId = entity.getFormId();
            if (formId != null) {
                Optional<FormInvoiceEntity> formOpt = formInvoiceRepository.findById(formId);
                if (formOpt.isPresent() && formOpt.get().getCategory() != null) {
                    entity.setCategory(formOpt.get().getCategory());
                } else {
                    throw new IllegalArgumentException("Không xác định được category từ form_invoices");
                }
            } else {
                throw new IllegalArgumentException("category không được null (cần form_id để suy ra category)");
            }
        }

        entity.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : LocalDateTime.now());
        return repository.save(entity);
    }

    @Override
    public Optional<InvoiceNumberEntity> findLatestByCompanyAndForm(Long companyId, Long formId) {
        return repository.findByCompanyIdAndFormId(companyId, formId)
                .stream()
                .sorted(Comparator.comparing(InvoiceNumberEntity::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .findFirst();
    }

    @Override
    public List<InvoiceNumberEntity> findActiveByCompany(Long companyId) {
        return repository.findByCompanyIdAndStatus(companyId, 1);
    }

    @Override
    public InvoiceNumberEntity update(InvoiceNumberEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity);
    }
}
