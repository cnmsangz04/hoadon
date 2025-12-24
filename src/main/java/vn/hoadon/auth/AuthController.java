package vn.hoadon.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.hoadon.services.UserService;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.dto.AuthRequest;
import vn.hoadon.dto.AuthResponse;
import vn.hoadon.security.JwtUtil;
import vn.hoadon.repositories.UserRepository;

import java.security.MessageDigest;
import java.util.Optional;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {

        Optional<UserEntity> uOpt = userService.findByUsername(req.getUsername());
        if (uOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid credentials");

        UserEntity user = uOpt.get();
        if (user.getStatus() == null || user.getStatus() == 0)
            return ResponseEntity.status(403).body("Account inactive");

        boolean passwordMatch = passwordEncoder.matches(req.getPassword(), user.getPassword());

        if (!passwordMatch && looksLikeLegacy(user.getPassword())) {
            if (checkLegacy(user.getPassword(), req.getPassword())) {

                user.setPassword(passwordEncoder.encode(req.getPassword()));
                userRepository.save(user);
                passwordMatch = true;
            }
        }

        if (!passwordMatch)
            return ResponseEntity.status(401).body("Invalid credentials");

        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", user.getId(), user.getRole()));
    }

    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody AuthRequest req) {

        Optional<UserEntity> uOpt = userService.findByUsername(req.getUsername());
        if (uOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid credentials");

        UserEntity user = uOpt.get();
        if (user.getStatus() == null || user.getStatus() == 0)
            return ResponseEntity.status(403).body("Account inactive");

        Integer role = user.getRole();
        Byte adminType = user.getAdminType();
        boolean isRoot = role != null && role == 0;
        boolean isSystemAdmin = role != null && role == 1 && adminType != null && adminType == 1;
        boolean isAdminCompany = role != null && role == 1 && adminType != null && adminType == 2;

        if (isAdminCompany)
            return ResponseEntity.status(403).body("Admin company is not allowed to access admin area");

        if (!isRoot) {
            if (user.getAdminPassword() == null || user.getAdminPassword().isEmpty())
                return ResponseEntity.status(403).body("No admin password set");

            boolean adminPasswordMatch = passwordEncoder.matches(req.getPassword(), user.getAdminPassword());
            if (!adminPasswordMatch && looksLikeLegacy(user.getAdminPassword())) {
                if (checkLegacy(user.getAdminPassword(), req.getPassword())) {
                    user.setAdminPassword(passwordEncoder.encode(req.getPassword()));
                    userRepository.save(user);
                    adminPasswordMatch = true;
                }
            }

            if (!adminPasswordMatch)
                return ResponseEntity.status(401).body("Invalid admin credentials");
        }

        if (!(isRoot || isSystemAdmin))
            return ResponseEntity.status(403).body("Not an admin account");

        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", user.getId(), user.getRole()));
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
        // Plain text
        if (stored.equals(raw)) return true;
        // MD5 hex
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
