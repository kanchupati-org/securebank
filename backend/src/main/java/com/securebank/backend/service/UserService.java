package com.securebank.backend.service;

import com.securebank.backend.dto.RegisterRequest;
import com.securebank.backend.dto.UserResponse;
import com.securebank.backend.entity.User;
import com.securebank.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.securebank.backend.enums.UserRole;
import com.securebank.backend.exception.AccessDeniedException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(RegisterRequest request) {

        String passwordHash =
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordHash,
                UserRole.CUSTOMER
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    public UserResponse getCurrentUser(Long userId) {

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail()
    );
}

public UserResponse getUserById(
        Long requestedUserId,
        Long authenticatedUserId) {

    User authenticatedUser = userRepository.findById(authenticatedUserId)
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

    if (!requestedUserId.equals(authenticatedUserId)
            && authenticatedUser.getRole() != UserRole.ADMIN) {

        throw new AccessDeniedException("Access denied");
    }

    User user = userRepository.findById(requestedUserId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail()
    );
}

}