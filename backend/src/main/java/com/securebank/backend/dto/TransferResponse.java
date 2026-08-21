package com.securebank.backend.dto;

public record TransferResponse(
        Long transferId,
        String status
) {
}