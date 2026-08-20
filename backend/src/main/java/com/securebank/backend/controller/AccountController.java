package com.securebank.backend.controller;

import com.securebank.backend.dto.AccountResponse;
import com.securebank.backend.service.AccountService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long id,
            HttpSession session) {

        Object userIdObject = session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Long authenticatedUserId = (Long) userIdObject;

        AccountResponse response =
                accountService.getAccountById(
                        id,
                        authenticatedUserId
                );

        return ResponseEntity.ok(response);
    }
}