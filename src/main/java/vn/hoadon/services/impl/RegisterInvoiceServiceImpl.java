package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.dto.registerinvoice.RegisterInvoicePrefillDto;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.TaxAuthorityEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.LegalRepresentativeRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.services.RegisterInvoiceService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RegisterInvoiceServiceImpl implements RegisterInvoiceService {
    private final RegisterInvoiceRepository repository;
    private final CompanyRepository companyRepository;
    private final LegalRepresentativeRepository legalRepresentativeRepository;

    public RegisterInvoiceServiceImpl(RegisterInvoiceRepository repository,
                                      CompanyRepository companyRepository,
                                      LegalRepresentativeRepository legalRepresentativeRepository) {
        this.repository = repository;
        this.companyRepository = companyRepository;
        this.legalRepresentativeRepository = legalRepresentativeRepository;
    }

    @Override
    public Optional<RegisterInvoiceEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<RegisterInvoiceEntity> findByCompany(Long companyId) {
        return repository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    @Override
    public List<RegisterInvoiceEntity> findByUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public RegisterInvoiceEntity create(RegisterInvoiceEntity entity) {
        entity.setId(null);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public Optional<RegisterInvoiceEntity> update(Long id, RegisterInvoiceEntity patch) {
        return repository.findById(id).map(existing -> {
            existing.setDeclarationCode(patch.getDeclarationCode());
            existing.setDeclarationType(patch.getDeclarationType());
            existing.setFormPattern(patch.getFormPattern());
            existing.setDeclarationDate(patch.getDeclarationDate());
            existing.setCompanyName(patch.getCompanyName());
            existing.setTaxCode(patch.getTaxCode());
            existing.setTaxAuthorityCode(patch.getTaxAuthorityCode());
            existing.setTaxAuthorityName(patch.getTaxAuthorityName());
            existing.setContactName(patch.getContactName());
            existing.setContactPhone(patch.getContactPhone());
            existing.setContactEmail(patch.getContactEmail());
            existing.setContactAddress(patch.getContactAddress());
            existing.setCreatePlace(patch.getCreatePlace());
            existing.setEffectiveDate(patch.getEffectiveDate());
            existing.setInvoiceForms(patch.getInvoiceForms());
            existing.setInvoiceTypes(patch.getInvoiceTypes());
            existing.setSendMethods(patch.getSendMethods());
            existing.setTransferMethods(patch.getTransferMethods());
            existing.setDigitalCertificates(patch.getDigitalCertificates());
            existing.setSolutionProviders(patch.getSolutionProviders());
            existing.setTransmitProviders(patch.getTransmitProviders());
            existing.setSignedXml(patch.getSignedXml());
            existing.setSignatureInfo(patch.getSignatureInfo());
            existing.setSignDate(patch.getSignDate());
            existing.setStatus(patch.getStatus());
            existing.setResponseReceiveFile(patch.getResponseReceiveFile());
            existing.setResponseAcceptFile(patch.getResponseAcceptFile());
            existing.setUpdatedAt(java.time.LocalDateTime.now());
            return repository.save(existing);
        });
    }

    @Override
    @Transactional
    public Optional<RegisterInvoiceEntity> attachSignedXml(Long id, String signedXml, String signatureInfo) {
        return repository.findById(id).map(existing -> {
            existing.setSignedXml(signedXml);
            existing.setSignatureInfo(signatureInfo);
            existing.setSignDate(java.time.LocalDateTime.now());
            return repository.save(existing);
        });
    }

    @Override
    public Optional<RegisterInvoicePrefillDto> prefill(Long companyId, Integer declarationType, LocalDate effectiveDate) {
        Optional<CompanyEntity> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            return Optional.empty();
        }
        CompanyEntity company = companyOpt.get();

        RegisterInvoicePrefillDto dto = new RegisterInvoicePrefillDto();
        dto.setDeclarationType(declarationType != null ? declarationType : 1);
        dto.setEffectiveDate(effectiveDate);
        dto.setCompanyName(company.getName());
        dto.setTaxCode(company.getTaxcode());
        TaxAuthorityEntity city = company.getTaxAuthorityCity();
        dto.setTaxAuthorityName(city != null ? city.getName() : null);
        dto.setContactAddress(company.getAddress());
        dto.setContactEmail(company.getEmail());
        legalRepresentativeRepository.findByCompanyId(company.getId()).ifPresent(legal -> {
            dto.setLegalFullname(legal.getFullname());
            dto.setLegalPhone(legal.getPhone());
            dto.setLegalCitizenId(legal.getCitizenId());
            dto.setLegalPassportNo(legal.getPassportNo());
            dto.setLegalDateOfBirth(legal.getDateOfBirth());
            dto.setLegalGender(legal.getGender());
        });
        return Optional.of(dto);
    }
}
