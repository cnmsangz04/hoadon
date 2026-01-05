package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.dto.mail.MailTemplateDto;
import vn.hoadon.entity.MailTemplateEntity;
import vn.hoadon.entity.VatRatesEntity;

import java.util.List;

public interface MailTemplateRepository extends JpaRepository<MailTemplateEntity, Integer> {
    List<MailTemplateEntity> findByCompany_Id(Integer companyId);

    MailTemplateEntity getById(Integer id);
}
