package vn.hoadon.services;

import vn.hoadon.entity.UserEntity;
import java.util.Optional;

public interface UserService {
    Optional<UserEntity> findByUsername(String username);
}
