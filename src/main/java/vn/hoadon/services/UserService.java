package vn.hoadon.services;

import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.dto.user.UserDto;
import vn.hoadon.entity.UserEntity;

import java.util.Optional;

public interface UserService {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);

    // Get current user info (mapped to DTO)
    UserDto getCurrentUserInfo();

    // Cập nhật current user fields
    UserDto updateCurrentUser(UserDto update);

    // Cập nhật avatar for current user
    UserDto updateAvatar(MultipartFile avatar);

    // Change password for current user
    void changePassword(String currentPassword, String newPassword);
}