package com.securebank.backend.controller;

import com.securebank.backend.dto.RegisterRequest;
import com.securebank.backend.dto.UserResponse;
import com.securebank.backend.service.UserService;
import org.springframework.http.HttpStatus;
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
    public UserResponse createUser(@RequestBody RegisterRequest request) {
        return userService.createUser(request);
    }
}