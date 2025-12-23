package vn.hoadon.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.hoadon.entity.UserEntity;

public interface MemberService {
    Page<UserEntity> list(String keyword, Long roleId, Byte status, Pageable pageable);
    UserEntity saveOrUpdate(UserEntity incoming);
    void setLock(Long id, boolean lock);
    void resetPassword(Long id);
    void removeFromCompany(Long id);
}
