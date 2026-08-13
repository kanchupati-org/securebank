package com.securebank.backend.dto;

public record LoginRequest(
        String email,
        String password
) {
}