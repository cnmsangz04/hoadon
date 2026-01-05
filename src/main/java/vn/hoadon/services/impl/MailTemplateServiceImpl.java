package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import vn.hoadon.dto.mail.MailTemplateDto;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.MailTemplateEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.MailTemplateRepository;
import vn.hoadon.services.MailTemplateService;

import java.util.List;


@Service
public class MailTemplateServiceImpl implements MailTemplateService {

    private final MailTemplateRepository mailTemplateRepository;
    private final CompanyRepository companyRepository;

    @Autowired
    public MailTemplateServiceImpl(MailTemplateRepository mailTemplateRepository,
                                   CompanyRepository companyRepository) {
        this.mailTemplateRepository = mailTemplateRepository;
        this.companyRepository = companyRepository;
    }

    /* ================= GET BY COMPANY ================= */
    public List<MailTemplateDto> getByCompanyId(Integer companyId) {

        return mailTemplateRepository.findByCompany_Id(companyId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public MailTemplateDto getById(Integer id) {
        return toDto(mailTemplateRepository.getById(id));
    }

    /* ================= CREATE ================= */
    public MailTemplateDto create(MailTemplateDto dto) {

        CompanyEntity company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        MailTemplateEntity entity = new MailTemplateEntity();
        entity.setCompany(company);
        entity.setKey(dto.getKey());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setStatus(dto.getStatus());
        entity.setSystem(dto.getSystem());

        mailTemplateRepository.save(entity);
        return toDto(entity);
    }

    /* ================= UPDATE ================= */
    public MailTemplateDto update(Integer id, MailTemplateDto dto) {

        MailTemplateEntity entity = mailTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mail template not found"));

        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setStatus(dto.getStatus());
        entity.setSystem(dto.getSystem());

        mailTemplateRepository.save(entity);
        return toDto(entity);
    }

    /* ================= DELETE ================= */
    public void delete(Integer id) {
        mailTemplateRepository.deleteById(id);
    }

    /* ================= MAPPER ================= */
    private MailTemplateDto toDto(MailTemplateEntity entity) {

        MailTemplateDto dto = new MailTemplateDto();
        dto.setId(entity.getId());
        dto.setKey(entity.getKey());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setStatus(entity.getStatus());
        dto.setSystem(entity.getSystem());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCompany() != null) {
            dto.setCompanyId(entity.getCompany().getId());
            dto.setCompanyName(entity.getCompany().getName());
        }

        return dto;
    }
}