package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.dto.member.MemberUpsertRequest;

public interface MemberService {
    Page<UserEntity> list(String keyword, Long roleId, Byte status, Long companyId, Integer role, Pageable pageable);
    UserEntity saveOrUpdate(MemberUpsertRequest incoming);
    void setLock(Long id, boolean lock);
    String resetPassword(Long id);
    void removeFromCompany(Long id);
    UserEntity getById(Long id);
    void delete(Long id);

    String sendCredentials(Long id);
}