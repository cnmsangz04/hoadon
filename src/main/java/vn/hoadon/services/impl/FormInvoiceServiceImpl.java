package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.services.FormInvoiceService;

import java.util.Optional;

@Service
public class FormInvoiceServiceImpl implements FormInvoiceService {

    private final FormInvoiceRepository repo;

    @Autowired
    public FormInvoiceServiceImpl(FormInvoiceRepository repo) {
        this.repo = repo;
    }

    @Override
    public Page<FormInvoiceEntity> pageByCompany(Long companyId, Pageable pageable) {
        return repo.findByCompanyId(companyId, pageable);
    }

    @Override
    public Page<FormInvoiceEntity> pageByCompanySystem(Long companyId, int system, Pageable pageable) {
        return repo.findByCompanyIdAndSystem(companyId, system, pageable);
    }

    @Override
    public Page<FormInvoiceEntity> pageAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    public Optional<FormInvoiceEntity> findById(Long id) { return repo.findById(id); }

    @Override
    public FormInvoiceEntity create(FormInvoiceEntity e) { return repo.save(e); }

    @Override
    public Optional<FormInvoiceEntity> update(Long id, FormInvoiceEntity patch) {
        return repo.findById(id).map(ex -> {
            // patch fields
            ex.setName(patch.getName() != null ? patch.getName() : ex.getName());
            ex.setSerial(patch.getSerial() != null ? patch.getSerial() : ex.getSerial());
            ex.setFile(patch.getFile() != null ? patch.getFile() : ex.getFile());
            ex.setPhoto(patch.getPhoto() != null ? patch.getPhoto() : ex.getPhoto());
            ex.setSystem(patch.getSystem() != null ? patch.getSystem() : ex.getSystem());
            ex.setType(patch.getType() != null ? patch.getType() : ex.getType());
            ex.setCategory(patch.getCategory() != null ? patch.getCategory() : ex.getCategory());
            ex.setStatus(patch.getStatus() != null ? patch.getStatus() : ex.getStatus());
            ex.setUpdatedAt(java.time.LocalDateTime.now());
            return repo.save(ex);
        });
    }

    @Override
    public void delete(Long id) { repo.deleteById(id); }
}