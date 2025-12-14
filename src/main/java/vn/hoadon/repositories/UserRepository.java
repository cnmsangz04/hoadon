package vn.hoadon.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import vn.hoadon.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);
}
