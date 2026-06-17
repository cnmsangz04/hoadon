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

    /** Look up company-specific templates by key and companyId. */
    @Query("SELECT t FROM MailTemplateEntity t WHERE t.key = :key AND t.company.id = :companyId ORDER BY t.status DESC, t.id ASC")
    List<MailTemplateEntity> findCompanyTemplatesByKeyAndCompanyId(@Param("key") String key, @Param("companyId") Integer companyId);

    default MailTemplateEntity findByKeyAndCompanyId(String key, Integer companyId) {
        List<MailTemplateEntity> list = findCompanyTemplatesByKeyAndCompanyId(key, companyId);
        for (MailTemplateEntity template : list) {
            if (template.getStatus() != null && template.getStatus() == 1) {
                return template;
            }
        }
        return list.isEmpty() ? null : list.get(0);
    }

    /** Look up a system-wide template (system = 1) by key, ignoring company. */
    @Query("SELECT t FROM MailTemplateEntity t WHERE t.key = :key AND t.system = 1 ORDER BY t.id ASC")
    List<MailTemplateEntity> findSystemTemplatesByKey(@Param("key") String key);

    default MailTemplateEntity findSystemByKey(String key) {
        List<MailTemplateEntity> list = findSystemTemplatesByKey(key);
        for (MailTemplateEntity template : list) {
            if (template.getStatus() != null && template.getStatus() == 1) {
                return template;
            }
        }
        return list.isEmpty() ? null : list.get(0);
    }
}
