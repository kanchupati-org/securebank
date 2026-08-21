package com.securebank.backend.service;

import com.securebank.backend.dto.TransferRequest;
import com.securebank.backend.entity.TransferIdempotency;

public interface IdempotencyService {

    TransferIdempotency createOrGet(
            String idempotencyKey,
            Long authenticatedUserId,
            TransferRequest request
    );

    void markCompleted(
            TransferIdempotency record,
            Long transferId
    );
}