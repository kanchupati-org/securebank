package com.securebank.backend.controller;

import com.securebank.backend.dto.RegisterRequest;
import com.securebank.backend.dto.UserResponse;
import com.securebank.backend.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody RegisterRequest request) {

        return userService.createUser(request);
    }


    @GetMapping("/me")
        public ResponseEntity<UserResponse> getCurrentUser(
        HttpSession session) {

    Object userId = session.getAttribute("userId");

    if (userId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .build();
    }

    UserResponse user =
            userService.getCurrentUser((Long) userId);

    return ResponseEntity.ok(user);
}

@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUserById(
        @PathVariable Long id,
        HttpSession session) {

    Object userIdObject = session.getAttribute("userId");

    if (userIdObject == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .build();
    }

    Long authenticatedUserId = (Long) userIdObject;

    UserResponse user =
            userService.getUserById(id, authenticatedUserId);

    return ResponseEntity.ok(user);
}

}