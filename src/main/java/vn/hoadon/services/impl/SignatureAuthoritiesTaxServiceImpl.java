package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import vn.hoadon.dto.SignatureAuthoritiesTaxDTO;
import vn.hoadon.entity.SignatureAuthoritiesTaxEntity;
import vn.hoadon.repositories.SignatureAuthoritiesTaxRepository;
import vn.hoadon.services.SignatureAuthoritiesTaxService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SignatureAuthoritiesTaxServiceImpl implements SignatureAuthoritiesTaxService {

    @Autowired
    private SignatureAuthoritiesTaxRepository repository;

    private SignatureAuthoritiesTaxDTO toDto(SignatureAuthoritiesTaxEntity e) {
        if (e == null) return null;
        SignatureAuthoritiesTaxDTO d = new SignatureAuthoritiesTaxDTO();
        d.id = e.getId();
        d.companyId = e.getCompanyId();
        d.invoiceId = e.getInvoiceId();
        d.xml = e.getXml();
        d.createdAt = e.getCreatedAt();
        d.updatedAt = e.getUpdatedAt();
        return d;
    }

    private SignatureAuthoritiesTaxEntity fromDto(SignatureAuthoritiesTaxDTO d) {
        if (d == null) return null;
        SignatureAuthoritiesTaxEntity e = new SignatureAuthoritiesTaxEntity();
        e.setId(d.id);
        e.setCompanyId(d.companyId);
        e.setInvoiceId(d.invoiceId);
        e.setXml(d.xml);
        e.setCreatedAt(d.createdAt);
        e.setUpdatedAt(d.updatedAt);
        return e;
    }

    @Override
    public SignatureAuthoritiesTaxDTO create(SignatureAuthoritiesTaxDTO dto) {
        SignatureAuthoritiesTaxEntity e = fromDto(dto);
        if (e.getCreatedAt() == null) e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        e = repository.save(e);
        return toDto(e);
    }

    @Override
    public SignatureAuthoritiesTaxDTO getLatestByInvoiceId(Integer invoiceId) {
        Optional<SignatureAuthoritiesTaxEntity> opt = repository.findTopByInvoiceIdOrderByIdDesc(invoiceId);
        return toDto(opt.orElse(null));
    }

    @Override
    public List<SignatureAuthoritiesTaxDTO> listByInvoiceId(Integer invoiceId) {
        return repository.findByInvoiceId(invoiceId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public SignatureAuthoritiesTaxDTO getLatestByInvoiceIdAndCompanyId(Integer invoiceId, Integer companyId) {
        Optional<SignatureAuthoritiesTaxEntity> opt = repository.findTopByInvoiceIdAndCompanyIdOrderByIdDesc(invoiceId, companyId);
        return toDto(opt.orElse(null));
    }

    @Override
    public List<SignatureAuthoritiesTaxDTO> listByInvoiceIdAndCompanyId(Integer invoiceId, Integer companyId) {
        return repository.findByInvoiceIdAndCompanyId(invoiceId, companyId).stream().map(this::toDto).collect(Collectors.toList());
    }
}
