package com.workforce.vms.controller;

import com.workforce.vms.model.User;
import com.workforce.vms.repository.UserRepository;
import com.workforce.vms.security.DatadogUserTracking;
import com.workforce.vms.security.JwtService;
import com.workforce.vms.security.PasswordHasher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Login endpoint — issues a JWT and seeds Datadog user attribution. */
@RestController
@RequestMapping("/login")
public class AuthController {

    private final UserRepository users;
    private final JwtService jwtService;

    public AuthController(UserRepository users, JwtService jwtService) {
        this.users = users;
        this.jwtService = jwtService;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    /**
     * POST /login {"username":"...","password":"..."}
     * Verifies the (weakly hashed) password and returns a bearer token.
     */
    @PostMapping
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> found = users.findById(req.username == null ? "" : req.username);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid credentials"));
        }
        User user = found.get();

        // DEMO-VULN: Weak crypto — password compared using unsalted MD5.
        String candidate = PasswordHasher.md5(req.password == null ? "" : req.password);
        if (!candidate.equalsIgnoreCase(user.getPasswordMd5())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid credentials"));
        }

        // Attribute this authenticated session to the Datadog trace immediately.
        DatadogUserTracking.setUser(user.getUsername(), user.getEmail(), user.getRole());

        String token = jwtService.issueToken(user.getUsername(), user.getEmail(), user.getRole());
        Map<String, Object> body = new HashMap<>();
        body.put("token", token);
        body.put("username", user.getUsername());
        body.put("role", user.getRole());
        return ResponseEntity.ok(body);
    }
}
