package com.securebank.backend.controller;

import com.securebank.backend.dto.LoginRequest;
import com.securebank.backend.entity.User;
import com.securebank.backend.repository.UserRepository;


import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
public ResponseEntity<?> login(
        @Valid @RequestBody LoginRequest request,HttpServletRequest httpRequest,
        HttpSession session) {


        User user = userRepository.findByEmail(request.email())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401)
                    .body("Invalid email or password");
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
    return ResponseEntity.status(401)
            .body("Invalid email or password");
}

httpRequest.changeSessionId();

session.setAttribute("userId", user.getId());

return ResponseEntity.ok(
        "Login successful"
);
    }

    

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {

        Object userId = session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(401)
                    .body("Not authenticated");
        }

        return ResponseEntity.ok(
                "Logged-in user ID: " + userId
        );
    }

    @PostMapping("/logout")
public ResponseEntity<?> logout(HttpSession session) {

    session.invalidate();

    return ResponseEntity.ok("Logout successful");
}
}