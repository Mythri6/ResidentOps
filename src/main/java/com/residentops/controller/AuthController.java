package com.residentops.controller;

import com.residentops.model.User;
import com.residentops.model.enums.Role;
import com.residentops.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for auth + user queries.
 * Registration is now handled via MvcController (admin-only).
 * This controller keeps login and user-list endpoints for the SPA.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");
        Optional<User> user = userService.login(email, password);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    /**
     * /api/auth/register is now DISABLED for self-registration.
     * All accounts are created by admin via /admin/people page.
     * This endpoint returns a clear message so the SPA shows the right info.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register() {
        return ResponseEntity.status(403).body(
                Map.of("error", "Self-registration is disabled. Please contact your admin to create an account.")
        );
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(userService.getByRole(Role.valueOf(role)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
