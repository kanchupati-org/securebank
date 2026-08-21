package com.securebank.backend.service;

import com.securebank.backend.dto.TransferRequest;
import com.securebank.backend.dto.TransferResponse;
import com.securebank.backend.entity.Account;
import com.securebank.backend.entity.Transfer;
import com.securebank.backend.entity.TransferIdempotency;
import com.securebank.backend.exception.AccessDeniedException;
import com.securebank.backend.repository.AccountRepository;
import com.securebank.backend.repository.TransferRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securebank.backend.exception.ResourceNotFoundException;
import com.securebank.backend.exception.InvalidTransferException;
import com.securebank.backend.exception.InsufficientBalanceException;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyService idempotencyService;

    // Test-only failure injection.
    private boolean failAfterTransfer;

    public TransferService(
        AccountRepository accountRepository,
        TransferRepository transferRepository,
        IdempotencyService idempotencyService) {

    this.accountRepository = accountRepository;
    this.transferRepository = transferRepository;
    this.idempotencyService = idempotencyService;
}

    /*
     * Test-only method.
     *
     * Package-private intentionally:
     * no public API is exposed for this.
     */
    void setFailAfterTransfer(boolean failAfterTransfer) {
        this.failAfterTransfer = failAfterTransfer;
    }

   @Transactional
public void transfer(
        TransferRequest request,
        Long authenticatedUserId) {

    executeTransfer(request, authenticatedUserId);
}

private Transfer executeTransfer(
        TransferRequest request,
        Long authenticatedUserId) {

    Long sourceId = request.fromAccountId();
    Long destinationId = request.toAccountId();

    // 1. Source and destination cannot be the same
    if (sourceId.equals(destinationId)) {
        throw new InvalidTransferException(
                "Source and destination accounts must be different"
        );
    }

    // 2. Determine deterministic lock order.
    //
    // Regardless of transfer direction:
    //
    // 1 -> 2 : lock 1, then 2
    // 2 -> 1 : lock 1, then 2
    //
    Long firstId = Math.min(sourceId, destinationId);
    Long secondId = Math.max(sourceId, destinationId);

    // 3. Lock BOTH accounts before reading either account. Performing an
    // ownership lookup first would cache a potentially stale Account in this
    // transaction's persistence context, defeating the later lock query.
    var lockedAccounts =
            accountRepository.findTwoAccountsForUpdate(
                    firstId,
                    secondId
            );

    if (lockedAccounts.size() != 2) {
        throw new ResourceNotFoundException(
                "One or more accounts not found"
        );
    }

    // 4. Get the locked source account.
    Account sourceAccount = lockedAccounts.stream()
            .filter(account ->
                    account.getId().equals(sourceId))
            .findFirst()
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Source account not found"
                    )
            );

    // 5. Get the locked destination account.
    Account destinationAccount = lockedAccounts.stream()
            .filter(account ->
                    account.getId().equals(destinationId))
            .findFirst()
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Destination account not found"
                    )
            );

    // 6. Authorize using the locked source account.
    if (!sourceAccount.getUserId()
            .equals(authenticatedUserId)) {

        throw new AccessDeniedException("Access denied");
    }

    // 7. Check sufficient balance.
    if (sourceAccount.getBalance()
            .compareTo(request.amount()) < 0) {

        throw new InsufficientBalanceException(
                "Insufficient balance"
        );
    }

    // 8. Debit source.
    sourceAccount.setBalance(
            sourceAccount.getBalance()
                    .subtract(request.amount())
    );

    // 9. Credit destination.
    destinationAccount.setBalance(
            destinationAccount.getBalance()
                    .add(request.amount())
    );

    // 10. Create transfer record.
    Transfer transfer = new Transfer(
            sourceAccount.getId(),
            destinationAccount.getId(),
            request.amount()
    );

    transferRepository.save(transfer);

    // 11. Test-only failure injection.
    if (failAfterTransfer) {
        throw new RuntimeException(
                "Simulated transfer failure"
        );
    }

    return transfer;
}
@Transactional
public TransferResponse transfer(
        TransferRequest request,
        Long authenticatedUserId,
        String idempotencyKey) {

    TransferIdempotency idempotency = idempotencyService.createOrGet(
            idempotencyKey,
            authenticatedUserId,
            request
    );

    if ("COMPLETED".equals(idempotency.getStatus())) {
        return new TransferResponse(
                idempotency.getTransferId(),
                "COMPLETED"
        );
    }

    Transfer transfer = executeTransfer(
            request,
            authenticatedUserId
    );

    idempotencyService.markCompleted(
            idempotency,
            transfer.getId()
    );

    return new TransferResponse(
            transfer.getId(),
            "COMPLETED"
    );
}

}
