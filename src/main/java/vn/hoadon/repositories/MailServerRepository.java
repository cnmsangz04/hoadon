package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoadon.entity.MailServerEntity;

import java.util.Optional;

public interface MailServerRepository extends JpaRepository<MailServerEntity, Integer> {

    /** Find the active mail server config for a company. */
    Optional<MailServerEntity> findFirstByCompanyIdAndStatusOrderByIdDesc(Integer companyId, Short status);
}
