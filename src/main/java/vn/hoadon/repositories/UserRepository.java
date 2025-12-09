package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import vn.hoadon.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
