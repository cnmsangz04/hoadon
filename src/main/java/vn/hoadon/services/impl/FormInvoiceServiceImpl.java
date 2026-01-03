package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceNumberEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceNumberRepository;
import vn.hoadon.services.FormInvoiceService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FormInvoiceServiceImpl implements FormInvoiceService {
    private static final Logger log = LoggerFactory.getLogger(FormInvoiceServiceImpl.class);

    private final FormInvoiceRepository repo;
    private final InvoiceNumberRepository invoiceNumberRepository;

    @Autowired
    public FormInvoiceServiceImpl(FormInvoiceRepository repo, InvoiceNumberRepository invoiceNumberRepository) {
        this.repo = repo;
        this.invoiceNumberRepository = invoiceNumberRepository;
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
    public FormInvoiceEntity create(FormInvoiceEntity e) {
        // Duplicate file and photo to new UUID filenames, update entity paths
        e.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt() : LocalDateTime.now());
        e.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt() : LocalDateTime.now());

        // Ensure default status is inactive (0) when not provided
        if (e.getStatus() == null) {
            e.setStatus(0);
        }
        log.debug("FormInvoice create request: companyId={}, category={}, status={}, serial={}", e.getCompanyId(), e.getCategory(), e.getStatus(), e.getSerial());

        // Validate duplicate serial for system=1 within same company & category
        if (e.getCompanyId() != null && e.getSystem() != null && e.getSystem() == 1 && e.getCategory() != null && e.getSerial() != null && !e.getSerial().isBlank()) {
            boolean exists = repo.existsByCompanyIdAndSystemAndCategoryAndSerial(e.getCompanyId(), 1, e.getCategory(), e.getSerial());
            if (exists) {
                throw new IllegalArgumentException("Ký hiệu đã tồn tại");
            }
        }

        // Helper to copy a single path
        java.util.function.Function<String, String> copyPath = (String original) -> {
            if (original == null || original.isBlank()) return original;
            try {
                Path src = Paths.get(original);
                if (!src.isAbsolute()) {
                    // Try relative to project root
                    src = Paths.get("").toAbsolutePath().resolve(original).normalize();
                }
                if (!Files.exists(src) || !Files.isRegularFile(src)) {
                    return original; // Leave as-is if source not found
                }
                String filename = src.getFileName().toString();
                String ext = "";
                int dot = filename.lastIndexOf('.');
                if (dot >= 0) ext = filename.substring(dot);
                String newName = UUID.randomUUID().toString() + ext;
                Path targetDir = src.getParent();
                if (targetDir == null) targetDir = src.toAbsolutePath().getParent();
                if (targetDir == null) targetDir = Paths.get("").toAbsolutePath();
                Path dst = targetDir.resolve(newName);
                Files.copy(src, dst);
                // Return path in same style as original (relative if original was relative)
                if (Paths.get(original).isAbsolute()) {
                    return dst.toString();
                } else {
                    Path projectRoot = Paths.get("").toAbsolutePath();
                    Path rel = projectRoot.relativize(dst);
                    return rel.toString().replace('\\','/');
                }
            } catch (Exception ex) {
                // If copy fails, keep original reference
                return original;
            }
        };

        if (e.getFile() != null) {
            e.setFile(copyPath.apply(e.getFile()));
        }
        if (e.getPhoto() != null) {
            e.setPhoto(copyPath.apply(e.getPhoto()));
        }

        FormInvoiceEntity saved = repo.save(e);
        log.debug("FormInvoice saved: id={}, companyId={}, category={}, status={}", saved.getId(), saved.getCompanyId(), saved.getCategory(), saved.getStatus());

        // If not active, do not touch invoice_numbers at all
        if (saved.getStatus() == null || saved.getStatus() != 1) {
            log.debug("Form not active (status={}), returning without touching invoice_numbers", saved.getStatus());
            return saved;
        }

        // Activation logic: enforce single active invoice_numbers per company+category
        if (saved.getCompanyId() != null && saved.getCategory() != null) {
            log.info("Activating form: id={}, companyId={}, category={}", saved.getId(), saved.getCompanyId(), saved.getCategory());
            List<InvoiceNumberEntity> activesByCat = invoiceNumberRepository.findByCompanyIdAndCategoryAndStatus(saved.getCompanyId(), saved.getCategory(), 1);
            log.debug("Found {} active invoice_numbers for companyId={}, category={}", activesByCat.size(), saved.getCompanyId(), saved.getCategory());
            if (!activesByCat.isEmpty()) {
                InvoiceNumberEntity inv = activesByCat.get(0);
                inv.setFormId(saved.getId());
                inv.setCategory(saved.getCategory());
                inv.setUpdatedAt(LocalDateTime.now());
                invoiceNumberRepository.save(inv);
                log.info("Updated active invoice_numbers id={} to form_id={}", inv.getId(), saved.getId());
            } else {
                InvoiceNumberEntity newInv = new InvoiceNumberEntity();
                newInv.setCompanyId(saved.getCompanyId());
                newInv.setFormId(saved.getId());
                newInv.setCategory(saved.getCategory());
                newInv.setStatus(1);
                newInv.setTotal(0);
                newInv.setCreatedAt(LocalDateTime.now());
                newInv.setUpdatedAt(LocalDateTime.now());
                invoiceNumberRepository.save(newInv);
                log.info("Created new active invoice_numbers for companyId={}, category={} pointing to form_id={}", saved.getCompanyId(), saved.getCategory(), saved.getId());
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public Optional<FormInvoiceEntity> update(Long id, FormInvoiceEntity patch) {
        return repo.findById(id).map(ex -> {
            Integer previousStatus = ex.getStatus();

            // Compute effective values to validate duplicate serials
            Long companyId = ex.getCompanyId();
            Integer system = patch.getSystem() != null ? patch.getSystem() : ex.getSystem();
            Integer category = patch.getCategory() != null ? patch.getCategory() : ex.getCategory();
            String serial = patch.getSerial() != null ? patch.getSerial() : ex.getSerial();
            if (companyId != null && system != null && system == 1 && category != null && serial != null && !serial.isBlank()) {
                boolean exists = repo.existsByCompanyIdAndSystemAndCategoryAndSerialAndIdNot(companyId, 1, category, serial, ex.getId());
                if (exists) {
                    throw new IllegalArgumentException("Ký hiệu đã tồn tại");
                }
            }

            ex.setName(patch.getName() != null ? patch.getName() : ex.getName());
            ex.setSerial(patch.getSerial() != null ? patch.getSerial() : ex.getSerial());
            ex.setFile(patch.getFile() != null ? patch.getFile() : ex.getFile());
            ex.setPhoto(patch.getPhoto() != null ? patch.getPhoto() : ex.getPhoto());
            ex.setSystem(patch.getSystem() != null ? patch.getSystem() : ex.getSystem());
            ex.setType(patch.getType() != null ? patch.getType() : ex.getType());
            ex.setCategory(patch.getCategory() != null ? patch.getCategory() : ex.getCategory());
            ex.setStatus(patch.getStatus() != null ? patch.getStatus() : ex.getStatus());
            ex.setUpdatedAt(LocalDateTime.now());

            log.debug("FormInvoice update: id={}, prevStatus={}, newStatus={}, companyId={}, category={}", ex.getId(), previousStatus, ex.getStatus(), ex.getCompanyId(), ex.getCategory());

            // Activation logic on status transition to 1
            if ((previousStatus == null || previousStatus != 1) && ex.getStatus() != null && ex.getStatus() == 1) {
                if (ex.getCompanyId() != null && ex.getCategory() != null) {
                    List<InvoiceNumberEntity> activesByCat = invoiceNumberRepository.findByCompanyIdAndCategoryAndStatus(ex.getCompanyId(), ex.getCategory(), 1);
                    log.debug("Update activation: found {} active invoice_numbers for companyId={}, category={}", activesByCat.size(), ex.getCompanyId(), ex.getCategory());
                    if (!activesByCat.isEmpty()) {
                        InvoiceNumberEntity inv = activesByCat.get(0);
                        inv.setFormId(ex.getId());
                        inv.setCategory(ex.getCategory());
                        inv.setUpdatedAt(LocalDateTime.now());
                        invoiceNumberRepository.save(inv);
                        log.info("Updated active invoice_numbers id={} to form_id={} on activation", inv.getId(), ex.getId());
                    } else {
                        InvoiceNumberEntity newInv = new InvoiceNumberEntity();
                        newInv.setCompanyId(ex.getCompanyId());
                        newInv.setFormId(ex.getId());
                        newInv.setCategory(ex.getCategory());
                        newInv.setStatus(1);
                        newInv.setTotal(0);
                        newInv.setCreatedAt(LocalDateTime.now());
                        newInv.setUpdatedAt(LocalDateTime.now());
                        invoiceNumberRepository.save(newInv);
                        log.info("Created new active invoice_numbers on activation for companyId={}, category={}, form_id={}", ex.getCompanyId(), ex.getCategory(), ex.getId());
                    }
                }
            } else {
                log.debug("No activation change (prevStatus={}, newStatus={}), skipping invoice_numbers changes", previousStatus, ex.getStatus());
            }

            return repo.save(ex);
        });
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}