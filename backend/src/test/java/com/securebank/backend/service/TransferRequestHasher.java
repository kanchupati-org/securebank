package com.securebank.backend.service;

import com.securebank.backend.dto.TransferRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

@Component
public class TransferRequestHasher {

    public String hash(
            TransferRequest request,
            Long authenticatedUserId) {

        String canonicalRequest =
                authenticatedUserId
                        + "|"
                        + request.fromAccountId()
                        + "|"
                        + request.toAccountId()
                        + "|"
                        + request.amount()
                                .stripTrailingZeros()
                                .toPlainString();

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            canonicalRequest.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {
                hex.append(
                        String.format("%02x", b)
                );
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    e
            );
        }
    }
}