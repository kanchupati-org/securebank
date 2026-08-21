package com.securebank.backend.controller;

import com.securebank.backend.dto.TransferRequest;
import com.securebank.backend.dto.TransferResponse;
import com.securebank.backend.service.TransferService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
public ResponseEntity<?> transfer(
        @RequestHeader(value = "Idempotency-Key", required = false)
        String idempotencyKey,
        @Valid @RequestBody TransferRequest request,
        HttpSession session) {

        Object userIdObject = session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Long authenticatedUserId = (Long) userIdObject;

        TransferResponse response = transferService.transfer(
        request,
        authenticatedUserId,
        idempotencyKey
);

return ResponseEntity.ok(response);
    }
}
