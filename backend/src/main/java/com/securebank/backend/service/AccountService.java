package com.securebank.backend.service;

import com.securebank.backend.dto.AccountResponse;
import com.securebank.backend.entity.Account;
import com.securebank.backend.entity.User;
import com.securebank.backend.enums.UserRole;
import com.securebank.backend.exception.AccessDeniedException;
import com.securebank.backend.repository.AccountRepository;
import com.securebank.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import com.securebank.backend.exception.AuthenticationException;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository) {

        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public AccountResponse getAccountById(
            Long accountId,
            Long authenticatedUserId) {

        User authenticatedUser = userRepository
        .findById(authenticatedUserId)
        .orElseThrow(() ->
                new AuthenticationException("Not authenticated"));
        Account account;

        if (authenticatedUser.getRole() == UserRole.ADMIN) {

            // ADMIN can access any account
            account = accountRepository
                    .findById(accountId)
                    .orElseThrow(() ->
                            new RuntimeException("Account not found"));

        } else {

            // CUSTOMER can access only their own account
            account = accountRepository
                    .findByIdAndUserId(
                            accountId,
                            authenticatedUserId)
                    .orElseThrow(() ->
                            new AccessDeniedException("Access denied"));
        }

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance()
        );
    }
}