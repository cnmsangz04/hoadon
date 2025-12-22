package vn.hoadon.services.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.UserService;
import vn.hoadon.dto.UserDto;
import vn.hoadon.entity.UserEntity;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/avatars";

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB

    @PostConstruct
    public void initUploadDir() {
        try {
            Path path = Paths.get(UPLOAD_DIR);
            Files.createDirectories(path);
            System.out.println("UPLOAD DIR READY: " + path.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory", e);
        }
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    private UserEntity getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Unauthenticated");
        }

        Object principal = auth.getPrincipal();

        if (!(principal instanceof UserEntity)) {
            throw new RuntimeException("Invalid principal");
        }

        return (UserEntity) principal;
    }

    private UserDto toDto(UserEntity e) {
        UserDto d = new UserDto();
        d.setId(e.getId());
        d.setUsername(e.getUsername());
        d.setName(e.getName());
        d.setEmail(e.getEmail());
        d.setPhone(e.getPhone());
        d.setAvatar(e.getAvatar());
        d.setRole(e.getRole());
        return d;
    }

    @Override
    public UserDto getCurrentUserInfo() {
        return toDto(getCurrentUserEntity());
    }

    @Override
    @Transactional
    public UserDto updateCurrentUser(UserDto update) {
        UserEntity e = getCurrentUserEntity();

        if (update.getName() != null) {
            e.setName(update.getName());
        }
        if (update.getEmail() != null) {
            e.setEmail(update.getEmail());
        }
        if (update.getPhone() != null) {
            e.setPhone(update.getPhone());
        }

        return toDto(userRepository.save(e));
    }

    @Override
    @Transactional
    public UserDto updateAvatar(MultipartFile avatar) {

        if (avatar == null || avatar.isEmpty()) {
            throw new RuntimeException("Avatar is empty");
        }

        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new RuntimeException("Avatar vượt quá 2MB");
        }

        String contentType = avatar.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("File không phải hình ảnh");
        }

        UserEntity user = getCurrentUserEntity();

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);

            String ext = Optional.ofNullable(avatar.getOriginalFilename())
                    .filter(f -> f.contains("."))
                    .map(f -> f.substring(f.lastIndexOf(".")))
                    .orElse(".png");

            String fileName = UUID.randomUUID() + ext;
            Path filePath = uploadPath.resolve(fileName);

            Files.write(
                filePath,
                avatar.getBytes(),
                StandardOpenOption.CREATE_NEW
            );

            user.setAvatar("/uploads/avatars/" + fileName);

            return toDto(userRepository.save(user));

        } catch (IOException ex) {
            throw new RuntimeException("Upload avatar failed", ex);
        }
    }
}
