package vn.hoadon.services;

import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.dto.UserDto;
import vn.hoadon.entity.UserEntity;

import java.util.Optional;

public interface UserService {
    Optional<UserEntity> findByUsername(String username);

    // Get current user info (mapped to DTO)
    UserDto getCurrentUserInfo();

    // Update current user fields
    UserDto updateCurrentUser(UserDto update);

    // Update avatar for current user
    UserDto updateAvatar(MultipartFile avatar);

    // Change password for current user
    void changePassword(String currentPassword, String newPassword);
}