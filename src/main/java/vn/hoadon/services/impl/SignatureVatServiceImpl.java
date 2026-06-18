package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import vn.hoadon.dto.SignatureVatDTO;
import vn.hoadon.entity.SignatureVatEntity;
import vn.hoadon.repositories.SignatureVatRepository;
import vn.hoadon.services.SignatureVatService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SignatureVatServiceImpl implements SignatureVatService {

    @Autowired
    private SignatureVatRepository repository;

    private SignatureVatDTO toDto(SignatureVatEntity e) {
        if (e == null) return null;
        SignatureVatDTO d = new SignatureVatDTO();
        d.id = e.getId();
        d.companyId = e.getCompanyId();
        d.invoiceId = e.getInvoiceId();
        d.xml = e.getXml();
        d.createdAt = e.getCreatedAt();
        d.updatedAt = e.getUpdatedAt();
        return d;
    }

    private SignatureVatEntity fromDto(SignatureVatDTO d) {
        if (d == null) return null;
        SignatureVatEntity e = new SignatureVatEntity();
        e.setId(d.id);
        e.setCompanyId(d.companyId);
        e.setInvoiceId(d.invoiceId);
        e.setXml(d.xml);
        e.setCreatedAt(d.createdAt);
        e.setUpdatedAt(d.updatedAt);
        return e;
    }

    @Override
    public SignatureVatDTO create(SignatureVatDTO dto) {
        SignatureVatEntity e = fromDto(dto);
        if (e.getCreatedAt() == null) e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        e = repository.save(e);
        return toDto(e);
    }

    @Override
    public SignatureVatDTO getLatestByInvoiceId(Integer invoiceId) {
        Optional<SignatureVatEntity> opt = repository.findTopByInvoiceIdOrderByIdDesc(invoiceId);
        return toDto(opt.orElse(null));
    }

    @Override
    public List<SignatureVatDTO> listByInvoiceId(Integer invoiceId) {
        return repository.findByInvoiceId(invoiceId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public SignatureVatDTO getLatestByInvoiceIdAndCompanyId(Integer invoiceId, Integer companyId) {
        Optional<SignatureVatEntity> opt = repository.findTopByInvoiceIdAndCompanyIdOrderByIdDesc(invoiceId, companyId);
        return toDto(opt.orElse(null));
    }

    @Override
    public List<SignatureVatDTO> listByInvoiceIdAndCompanyId(Integer invoiceId, Integer companyId) {
        return repository.findByInvoiceIdAndCompanyId(invoiceId, companyId).stream().map(this::toDto).collect(Collectors.toList());
    }
}
