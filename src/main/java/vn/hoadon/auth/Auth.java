package vn.hoadon.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.hoadon.services.UserService;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.dto.auth.AuthRequest;
import vn.hoadon.dto.auth.AuthResponse;
import vn.hoadon.security.JwtUtil;
import vn.hoadon.repositories.UserRepository;

import java.util.Optional;

@RestController
@RequestMapping("/v1/auth")
public class Auth {

    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {

        // Prefer email for lookup; fallback to username for backward compatibility
        Optional<UserEntity> uOpt = Optional.empty();
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            uOpt = userService.findByEmail(req.getEmail().trim());
        }
        if (uOpt.isEmpty() && req.getUsername() != null && !req.getUsername().isBlank()) {
            uOpt = userService.findByUsername(req.getUsername().trim());
        }
        if (uOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid credentials");

        UserEntity user = uOpt.get();
        if (user.getStatus() == null || user.getStatus() == 0)
            return ResponseEntity.status(403).body("Account inactive");

        boolean passwordMatch = passwordEncoder.matches(req.getPassword(), user.getPassword());
        if (!passwordMatch)
            return ResponseEntity.status(401).body("Invalid credentials");

        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", user.getId(), user.getRole()));
    }

    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody AuthRequest req) {

        Optional<UserEntity> uOpt = Optional.empty();
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            uOpt = userService.findByEmail(req.getEmail().trim());
        }
        if (uOpt.isEmpty() && req.getUsername() != null && !req.getUsername().isBlank()) {
            uOpt = userService.findByUsername(req.getUsername().trim());
        }
        if (uOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid credentials");

        UserEntity user = uOpt.get();
        if (user.getStatus() == null || user.getStatus() == 0)
            return ResponseEntity.status(403).body("Account inactive");

        Integer role = user.getRole();
        boolean isRoot = role != null && role == 0;
        boolean isSystemAdmin = role != null && role == 1;

        if (!isRoot) {
            if (user.getAdminPassword() == null || user.getAdminPassword().isEmpty())
                return ResponseEntity.status(403).body("No admin password set");

            boolean adminPasswordMatch = passwordEncoder.matches(req.getPassword(), user.getAdminPassword());
            if (!adminPasswordMatch)
                return ResponseEntity.status(401).body("Invalid admin credentials");
        }

        if (!(isRoot || isSystemAdmin))
            return ResponseEntity.status(403).body("Not an admin account");

        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", user.getId(), user.getRole()));
    }
}