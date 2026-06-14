package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoadon.dto.mail.MailTemplateDto;
import vn.hoadon.entity.MailTemplateEntity;

import java.util.List;

public interface MailTemplateRepository extends JpaRepository<MailTemplateEntity, Integer> {

    List<MailTemplateEntity> findByCompany_Id(Integer companyId);

    MailTemplateEntity getById(Integer id);

    /** Look up a company-specific template by key and companyId. */
    @Query("SELECT t FROM MailTemplateEntity t WHERE t.key = :key AND t.company.id = :companyId")
    MailTemplateEntity findByKeyAndCompanyId(@Param("key") String key, @Param("companyId") Integer companyId);

    /** Look up a system-wide template (system = 1) by key, ignoring company. */
    @Query("SELECT t FROM MailTemplateEntity t WHERE t.key = :key AND t.system = 1 ORDER BY t.id ASC")
    List<MailTemplateEntity> findSystemTemplatesByKey(@Param("key") String key);

    default MailTemplateEntity findSystemByKey(String key) {
        List<MailTemplateEntity> list = findSystemTemplatesByKey(key);
        return list.isEmpty() ? null : list.get(0);
    }
}

