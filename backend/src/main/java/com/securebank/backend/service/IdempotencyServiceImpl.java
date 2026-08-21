package com.securebank.backend.service;

import com.securebank.backend.dto.TransferRequest;
import com.securebank.backend.entity.TransferIdempotency;
import com.securebank.backend.exception.AccessDeniedException;
import com.securebank.backend.exception.IdempotencyConflictException;
import com.securebank.backend.repository.TransferIdempotencyRepository;

// import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private final TransferIdempotencyRepository repository;
    private final TransferRequestHasher hasher;

    public IdempotencyServiceImpl(
            TransferIdempotencyRepository repository,
            TransferRequestHasher hasher) {

        this.repository = repository;
        this.hasher = hasher;
    }

    @Override
@Transactional
public TransferIdempotency createOrGet(
        String idempotencyKey,
        Long authenticatedUserId,
        TransferRequest request) {

    String requestHash =
            hasher.hash(
                    request,
                    authenticatedUserId
            );

    /*
     * Atomically try to claim the idempotency key.
     *
     * 1 = this request created the record.
     * 0 = another request already owns the key.
     */
    int inserted =
            repository.tryCreate(
                    idempotencyKey,
                    authenticatedUserId,
                    requestHash
            );

    /*
     * This request successfully claimed the key.
     */
    if (inserted == 1) {

        return repository.findByIdempotencyKey(
                idempotencyKey
        ).orElseThrow();
    }

    /*
     * Another request already claimed this key.
     */
    TransferIdempotency record =
            repository.findByIdempotencyKey(
                    idempotencyKey
            ).orElseThrow();

    /*
     * Same idempotency key must belong
     * to the same authenticated user.
     */
    if (!record.getUserId()
            .equals(authenticatedUserId)) {

        throw new AccessDeniedException(
                "Idempotency key already belongs to another user"
        );
    }

    /*
     * Same idempotency key must represent
     * exactly the same request.
     */
    if (!record.getRequestHash()
            .equals(requestHash)) {

        throw new IdempotencyConflictException(
                "Idempotency key was already used for a different request"
        );
    }

    /*
     * Same key + same user + same request
     * = legitimate retry.
     */
    return record;
}

@Override
public void markCompleted(
        TransferIdempotency record,
        Long transferId) {

    record.markCompleted(transferId);

    repository.save(record);
}

}
