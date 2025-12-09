package vn.hoadon.services;

import vn.hoadon.model.User;
import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
}
