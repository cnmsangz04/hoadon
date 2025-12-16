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

import java.util.Optional;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {

        Optional<UserEntity> uOpt = userService.findByUsername(req.getUsername());
        if (uOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid credentials");

        UserEntity user = uOpt.get();

        if (user.getStatus() == null || user.getStatus() == 0)
            return ResponseEntity.status(403).body("Account inactive");

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            return ResponseEntity.status(401).body("Invalid credentials");

        String token = jwtUtil.generateToken(user);

        return ResponseEntity.ok(
            new AuthResponse(token, "Bearer", user.getId(), user.getRole())
        );
    }

    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody AuthRequest req) {

        Optional<UserEntity> uOpt = userService.findByUsername(req.getUsername());
        if (uOpt.isEmpty())
            return ResponseEntity.status(401).body("Invalid credentials");

        UserEntity user = uOpt.get();

        if (user.getStatus() == null || user.getStatus() == 0)
            return ResponseEntity.status(403).body("Account inactive");

        if (user.getAdminPassword() == null || user.getAdminPassword().isEmpty())
            return ResponseEntity.status(403).body("No admin password set");

        if (!passwordEncoder.matches(req.getPassword(), user.getAdminPassword()))
            return ResponseEntity.status(401).body("Invalid admin credentials");

        // admin = 0 hoặc 1
        if (user.getRole() == null || (user.getRole() != 0 && user.getRole() != 1))
            return ResponseEntity.status(403).body("Not an admin account");

        String token = jwtUtil.generateToken(user);

        return ResponseEntity.ok(
            new AuthResponse(token, "Bearer", user.getId(), user.getRole())
        );
    }
}
