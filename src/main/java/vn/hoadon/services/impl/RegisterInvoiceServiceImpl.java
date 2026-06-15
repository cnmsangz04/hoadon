package vn.hoadon.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.dto.registerinvoice.RegisterInvoicePrefillDto;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.TaxAuthorityEntity;
import vn.hoadon.entity.LegalRepresentativeEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.LegalRepresentativeRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.services.RegisterInvoiceService;
import vn.hoadon.util.RegisterInvoiceXmlBuilder;

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
            // Cập nhật status to 1 (signed) as required by spec
            existing.setStatus(1);
            existing.setUpdatedAt(java.time.LocalDateTime.now());
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

    @Override
    public Page<RegisterInvoiceEntity> pageByCompany(Long companyId, Pageable pageable) {
        return repository.findByCompanyId(companyId, pageable);
    }

    @Override
    public Page<RegisterInvoiceEntity> pageByCompanyAndStatus(Long companyId, Integer status, Pageable pageable) {
        return repository.findByCompanyIdAndStatus(companyId, status, pageable);
    }

    @Override
    public Page<RegisterInvoiceEntity> pageAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public String buildUnsignedXml(RegisterInvoiceEntity entity) {
        String contactName = null;
        String contactPhone = null;
        String contactEmail = null;
        String contactAddress = null;
        String citizenId = null;
        String passportNo = null;
        String dateOfBirth = null;
        String gender = null;
        String taxAuthorityCode = null;
        String taxAuthorityName = null;
        String companyName = "";
        String taxCode = "";
        CompanyEntity company = companyRepository.findById(entity.getCompanyId()).orElse(null);
        LegalRepresentativeEntity legal = legalRepresentativeRepository.findByCompanyId(entity.getCompanyId()).orElse(null);
        if (company != null) {
            companyName = company.getName() != null ? company.getName() : "";
            taxCode = company.getTaxcode() != null ? company.getTaxcode() : "";
            contactEmail = company.getEmail();
            contactAddress = company.getAddress();
            TaxAuthorityEntity ta = company.getTaxAuthority();
            if (ta != null) {
                taxAuthorityCode = ta.getCode() != null ? String.valueOf(ta.getCode()) : "";
                taxAuthorityName = ta.getName() != null ? ta.getName() : "";
            } else if (company.getTaxAuthorityCity() != null) {
                taxAuthorityCode = company.getTaxAuthorityCity().getCode() != null ? String.valueOf(company.getTaxAuthorityCity().getCode()) : "";
                taxAuthorityName = company.getTaxAuthorityCity().getName() != null ? company.getTaxAuthorityCity().getName() : "";
            } else {
                taxAuthorityCode = "";
                taxAuthorityName = "";
            }
        }
        if (legal != null) {
            contactName = legal.getFullname();
            contactPhone = legal.getPhone();
            citizenId = legal.getCitizenId();
            passportNo = legal.getPassportNo();
            dateOfBirth = legal.getDateOfBirth() != null ? legal.getDateOfBirth().toString() : null;
            gender = legal.getGender() != null ? String.valueOf(legal.getGender()) : null;
        }
        return RegisterInvoiceXmlBuilder.buildUnsigned(
                entity,
                contactName,
                contactPhone,
                contactEmail,
                contactAddress,
                citizenId,
                passportNo,
                dateOfBirth,
                gender,
                taxAuthorityCode,
                taxAuthorityName,
                companyName,
                taxCode
        );
    }

    @Override
    public Optional<String> getXmlForDownload(Long id) {
        return repository.findById(id).map(e -> {
            String signed = e.getSignedXml();
            if (signed != null && !signed.isBlank()) {
                return signed;
            }
            // Dự phòng: build unsigned XML from legal/company info
            return buildUnsignedXml(e);
        });
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}