package vn.hoadon.controller.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.hoadon.services.UserService;
import vn.hoadon.model.User;
import vn.hoadon.dto.AuthRequest;
import vn.hoadon.dto.AuthResponse;
import vn.hoadon.security.JwtUtil;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req){
        Optional<User> uOpt = userService.findByUsername(req.getUsername());
        if(uOpt.isEmpty()) return ResponseEntity.status(401).body("Invalid credentials");

        User user = uOpt.get();
        if(user.getStatus() == null || user.getStatus() == 0) {
            return ResponseEntity.status(403).body("Account inactive");
        }

        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())){
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        Long uid = user.getId() != null ? user.getId().longValue() : null;
        Integer role = user.getRole() != null ? user.getRole().intValue() : null;
        String token = jwtUtil.generateToken(uid, user.getUsername(), role);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", uid, role));
    }

    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody AuthRequest req){
        Optional<User> uOpt = userService.findByUsername(req.getUsername());
        if(uOpt.isEmpty()) return ResponseEntity.status(401).body("Invalid credentials");
        User user = uOpt.get();
        if(user.getStatus() == null || user.getStatus() == 0) {
            return ResponseEntity.status(403).body("Account inactive");
        }

        // check admin_password exists
        String adminPassHash = user.getAdminPassword();
        if(adminPassHash == null || adminPassHash.isEmpty()){
            return ResponseEntity.status(403).body("No admin password set");
        }
        if(!passwordEncoder.matches(req.getPassword(), adminPassHash)){
            return ResponseEntity.status(401).body("Invalid admin credentials");
        }

        // Also check role is admin/root
        if(user.getRole() == null || (user.getRole() != 0 && user.getRole() != 1)){
            return ResponseEntity.status(403).body("Not an admin account");
        }

        Long uid = user.getId() != null ? user.getId().longValue() : null;
        Integer role = user.getRole() != null ? user.getRole().intValue() : null;
        String token = jwtUtil.generateToken(uid, user.getUsername(), role);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", uid, role));
    }
}