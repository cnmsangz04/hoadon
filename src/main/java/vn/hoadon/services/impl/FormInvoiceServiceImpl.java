package vn.hoadon.services.impl;

import vn.hoadon.invoice.template.persistence.CompanyInvoiceDefaultEntity;
import vn.hoadon.invoice.template.persistence.CompanyInvoiceDefaultRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceNumberEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceNumberRepository;
import vn.hoadon.services.FormInvoiceService;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FormInvoiceServiceImpl implements FormInvoiceService {
    private final FormInvoiceRepository repo;
    private final InvoiceNumberRepository invoiceNumberRepository;
    @Autowired private CompanyRepository companies;
    @Autowired private InvoiceRepository invoices;
    @Autowired private RegisterInvoiceRepository declarations;
    @Autowired private CompanyInvoiceDefaultRepository defaults;
    @Autowired private vn.hoadon.invoice.template.service.InvoiceFormTemplateService formTemplates;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

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
    public Page<FormInvoiceEntity> pageByCompanySystemWithFilters(Long companyId, Integer system, String q, Integer category, Integer type, Integer status, Pageable pageable) {
        Specification<FormInvoiceEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (companyId != null) {
                predicates.add(cb.equal(root.get("companyId"), companyId));
            }
            if (system != null) {
                predicates.add(cb.equal(root.get("system"), system));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String kw = "%" + q.toLowerCase() + "%";
                Predicate namePred = cb.like(cb.lower(root.get("name")), kw);
                Predicate serialPred = cb.like(cb.lower(cb.concat(cb.coalesce(root.get("formCode"), ""), cb.coalesce(root.get("serial"), ""))), kw);
                Predicate formCodePred = cb.like(cb.lower(root.get("formCode")), kw);
                predicates.add(cb.or(namePred, serialPred, formCodePred));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return repo.findAll(spec, pageable);
    }

    @Override
    public Page<FormInvoiceEntity> pageBySystem(int system, Pageable pageable) {
        return repo.findBySystem(system, pageable);
    }

    @Override
    public Page<FormInvoiceEntity> pageAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    public Page<FormInvoiceEntity> pageByUser(Long userId, Pageable pageable) {
        return repo.findByUserId(userId, pageable);
    }

    @Override
    public Optional<FormInvoiceEntity> findById(Long id) { return repo.findById(id); }

    @Override
    @Transactional
    public FormInvoiceEntity create(FormInvoiceEntity form) {
        if (form.getStatus() == null) form.setStatus(0);
        lockCompany(form);
        validate(form);
        form.setCreatedAt(LocalDateTime.now());
        form.setUpdatedAt(form.getCreatedAt());
        FormInvoiceEntity saved = repo.saveAndFlush(form);
        var access = new vn.hoadon.invoice.template.service.TemplateAccess(saved.getCompanyId(), saved.getUserId(), Integer.valueOf(0).equals(saved.getSystem()));
        formTemplates.editable(saved.getId(), "form-" + saved.getId(), access);
        synchronizeActivation(saved, false);
        return saved;
    }

    @Override
    @Transactional
    public Optional<FormInvoiceEntity> update(Long id, FormInvoiceEntity patch) {
        FormInvoiceEntity current = repo.findById(id).orElse(null);
        if (current == null) return Optional.empty();
        lockCompany(current);
        entityManager.refresh(current);
        boolean wasActive = Integer.valueOf(1).equals(current.getStatus());
        if (patch.getSystem() != null && !patch.getSystem().equals(current.getSystem())) throw new IllegalArgumentException("Không được thay đổi phạm vi mẫu");
        if (patch.getCompanyId() != null && !patch.getCompanyId().equals(current.getCompanyId())) throw new IllegalArgumentException("Không được chuyển mẫu sang công ty khác");
        boolean fiscalChanged = changed(patch.getFormCode(), current.getFormCode()) || changed(patch.getSerial(), current.getSerial())
                || changed(patch.getCategory(), current.getCategory()) || changed(patch.getType(), current.getType()) || changed(patch.getHaveCode(), current.getHaveCode());
        if (fiscalChanged && invoices.existsByFormId(Math.toIntExact(id))) throw new IllegalArgumentException("Mẫu đã được sử dụng; không được sửa ký hiệu hoặc cấu hình phát hành");
        if (patch.getName() != null) current.setName(patch.getName());
        if (patch.getFormCode() != null) current.setFormCode(patch.getFormCode());
        if (patch.getSerial() != null) current.setSerial(patch.getSerial());
        if (patch.getPhoto() != null) current.setPhoto(patch.getPhoto());
        if (patch.getType() != null) current.setType(patch.getType());
        if (patch.getCategory() != null) current.setCategory(patch.getCategory());
        if (patch.getStatus() != null) current.setStatus(patch.getStatus());
        if (patch.getHaveCode() != null) current.setHaveCode(patch.getHaveCode());
        current.setUpdatedAt(LocalDateTime.now());
        boolean deactivating = !fiscalChanged && Integer.valueOf(0).equals(patch.getStatus());
        if (!deactivating) validate(current);
        synchronizeActivation(current, wasActive);
        return Optional.of(repo.save(current));
    }

    private void lockCompany(FormInvoiceEntity form) {
        if (Integer.valueOf(1).equals(form.getSystem())) companies.lockById(form.getCompanyId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty"));
    }

    private void validate(FormInvoiceEntity form) {
        if (form.getName() == null || form.getName().isBlank() || form.getName().length() > 255) throw new IllegalArgumentException("Tên mẫu không hợp lệ");
        if (!java.util.Set.of(0, 1).contains(form.getStatus())) throw new IllegalArgumentException("Trạng thái mẫu không hợp lệ");
        if (form.getCategory() == null || !java.util.Set.of(1, 2).contains(form.getCategory()) || form.getType() == null || !java.util.Set.of(1, 2).contains(form.getType())) throw new IllegalArgumentException("Loại hóa đơn hoặc loại thuế suất không hợp lệ");
        if (form.getHaveCode() == null || !java.util.Set.of(0, 1).contains(form.getHaveCode())) throw new IllegalArgumentException("Hình thức có mã/không mã không hợp lệ");
        if (Integer.valueOf(1).equals(form.getStatus()) && form.getTemplateVersionId() == null) throw new IllegalArgumentException("Cần công bố và chọn phiên bản thiết kế trước khi kích hoạt mẫu hóa đơn");
        if (!Integer.valueOf(1).equals(form.getSystem())) return;
        if (!String.valueOf(form.getCategory()).equals(form.getFormCode()) || form.getSerial() == null || !form.getSerial().matches("[CK][0-9]{2}T[A-Z]{2}")) throw new IllegalArgumentException("Ký hiệu hóa đơn không hợp lệ");
        if ((form.getSerial().startsWith("C") ? 1 : 0) != form.getHaveCode()) throw new IllegalArgumentException("Ký hiệu C/K không khớp hình thức hóa đơn");
        if (Integer.valueOf(1).equals(form.getStatus())) {
            var accepted = declarations.findLatestAccepted(form.getCompanyId(), org.springframework.data.domain.PageRequest.of(0, 1));
            if (accepted.isEmpty()) throw new IllegalArgumentException("Chưa có tờ khai hóa đơn được chấp nhận");
            var declaration = accepted.getFirst();
            String kind = form.getCategory() == 1 ? "HDGTGT" : "HDBHang";
            if (!contains(declaration.getInvoiceTypes(), kind) || !contains(declaration.getInvoiceForms(), form.getHaveCode() == 1 ? "CMa" : "KCMa")) throw new IllegalArgumentException("Tờ khai không phù hợp loại hoặc hình thức hóa đơn");
        }
    }

    private static boolean contains(List<String> values, String value) { return values != null && values.contains(value); }

    private void synchronizeActivation(FormInvoiceEntity form, boolean wasActive) {
        if (!Integer.valueOf(1).equals(form.getSystem())) return;
        boolean active = Integer.valueOf(1).equals(form.getStatus());
        if (active) repo.deactivateOthers(form.getCompanyId(), form.getCategory(), form.getId());
        var selection = defaults.findByCompanyIdAndCategory(form.getCompanyId(), form.getCategory()).orElse(null);
        if (selection == null) {
            if (!active) return;
            selection = new CompanyInvoiceDefaultEntity();
            selection.setCompanyId(form.getCompanyId());
            selection.setCategory(form.getCategory());
            selection.setActivationPolicy("COMPAT_SINGLE_ACTIVE_PER_CATEGORY");
        }
        if (active) selection.setFormInvoiceId(form.getId());
        else if (java.util.Objects.equals(selection.getFormInvoiceId(), form.getId())) selection.setFormInvoiceId(null);
        selection.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        defaults.save(selection);
        if (!active || wasActive) return;
        // Giữ bộ đếm hiện hành đến khi dữ liệu dãy số được đối chiếu và chuyển đổi.
        List<InvoiceNumberEntity> counters = invoiceNumberRepository.findByCompanyIdAndCategoryAndStatus(form.getCompanyId(), form.getCategory(), 1);
        InvoiceNumberEntity counter = counters.isEmpty() ? new InvoiceNumberEntity() : counters.getFirst();
        if (counter.getId() == null) {
            counter.setCompanyId(form.getCompanyId());
            counter.setCategory(form.getCategory());
            counter.setTotal(0);
            counter.setStatus(1);
            counter.setCreatedAt(LocalDateTime.now());
        }
        counter.setFormId(form.getId());
        counter.setUpdatedAt(LocalDateTime.now());
        invoiceNumberRepository.save(counter);
    }

    private static boolean changed(Object patch, Object current) { return patch != null && !patch.equals(current); }

    @Override
    @Transactional
    public void delete(Long id) {
        var form = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu"));
        lockCompany(form);
        if (!Integer.valueOf(0).equals(form.getStatus()) || invoices.existsByFormId(Math.toIntExact(id)) || !invoiceNumberRepository.findByCompanyIdAndFormId(form.getCompanyId(), id).isEmpty()) throw new IllegalArgumentException("Mẫu đang được sử dụng; hãy ngừng kích hoạt thay vì xóa");
        repo.delete(form);
    }
}
