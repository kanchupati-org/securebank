package com.securebank.backend.controller;

import com.securebank.backend.dto.LoginRequest;
import com.securebank.backend.dto.UserResponse;
import com.securebank.backend.entity.User;
import com.securebank.backend.repository.UserRepository;


import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        Map.of("message", "Login successful")
);
    }

    

    @GetMapping("/me")
public ResponseEntity<?> me(HttpSession session) {

    Object userIdObject = session.getAttribute("userId");

    if (userIdObject == null) {
        return ResponseEntity.status(401)
                .body("Not authenticated");
    }

    Long userId = (Long) userIdObject;

    User user = userRepository.findById(userId)
            .orElse(null);

    if (user == null) {
        session.invalidate();

        return ResponseEntity.status(401)
                .body("Not authenticated");
    }

    return ResponseEntity.ok(
            Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail()
            )
    );
}

    @PostMapping("/logout")
public ResponseEntity<?> logout(HttpSession session) {

    session.invalidate();

    return ResponseEntity.ok("Logout successful");
}



}