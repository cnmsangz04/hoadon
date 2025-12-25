package vn.hoadon.services.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.UserService;
import vn.hoadon.dto.user.UserDto;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.util.UploadPath;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;
import java.security.MessageDigest;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB

    @PostConstruct
    public void initUploadDir() {
        try {
            // Ensure base uploads dir exists
            UploadPath.baseDir();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory", e);
        }
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email);
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

    private String expectedUsername(Long id) {
        return id == null ? null : ("cp-" + id);
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

        // Block username editing: always enforce rule
        String expect = expectedUsername(e.getId());
        if (expect != null && !expect.equals(e.getUsername())) {
            e.setUsername(expect);
        }

        if (update.getName() != null) {
            e.setName(update.getName());
        }
        if (update.getEmail() != null) {
            // normalize for comparison
            String newEmail = update.getEmail().trim();
            if (!newEmail.isEmpty()) {
                // remove duplicate email check to allow non-unique emails
                e.setEmail(newEmail);
            } else {
                e.setEmail(null);
            }
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
            Long companyId = user.getCompanyId();
            Path uploadPath = UploadPath.resolveCompanyTypeDir(companyId, "avatars");

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

            user.setAvatar(UploadPath.publicUrl(companyId, "avatars", fileName));

            return toDto(userRepository.save(user));

        } catch (IOException ex) {
            throw new RuntimeException("Upload avatar failed", ex);
        }
    }

    @Override
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Mật khẩu mới phải từ 8 ký tự");
        }
        UserEntity user = getCurrentUserEntity();
        String stored = user.getPassword();
        boolean match = passwordEncoder.matches(currentPassword, stored);
        if (!match && looksLikeLegacy(stored)) {
            if (checkLegacy(stored, currentPassword)) {
                match = true;
            }
        }
        if (!match) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private boolean looksLikeLegacy(String stored) {
        if (stored == null || stored.isBlank()) return false;
        return !looksLikeBcrypt(stored);
    }

    private boolean looksLikeBcrypt(String v) {
        return v != null && v.length() == 60 && v.startsWith("$2") && v.matches("^\\$2[aby]\\$\\d{2}\\$.*");
    }

    private boolean checkLegacy(String stored, String raw) {
        if (stored == null || raw == null) return false;
        if (stored.equals(raw)) return true; // plain
        return isHex32(stored) && stored.equalsIgnoreCase(md5Hex(raw));
    }

    private boolean isHex32(String s) {
        return s != null && s.length() == 32 && s.matches("[0-9a-fA-F]{32}");
    }

    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}