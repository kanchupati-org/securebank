package com.securebank.backend.service;

import com.securebank.backend.dto.TransferRequest;
import com.securebank.backend.entity.Account;
import com.securebank.backend.entity.Transfer;
import com.securebank.backend.exception.AccessDeniedException;
import com.securebank.backend.repository.AccountRepository;
import com.securebank.backend.repository.TransferRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securebank.backend.exception.ResourceNotFoundException;
import com.securebank.backend.exception.InvalidTransferException;
import com.securebank.backend.exception.InsufficientBalanceException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    // Test-only failure injection.
    private boolean failAfterTransfer;

    public TransferService(
            AccountRepository accountRepository,
            TransferRepository transferRepository) {

        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
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

    System.out.println(
            "TRANSACTION ACTIVE = "
                    + TransactionSynchronizationManager
                            .isActualTransactionActive()
    );


        // 1. Source account must belong to authenticated user
        Account sourceAccount = accountRepository
                .findByIdAndUserId(
                        request.fromAccountId(),
                        authenticatedUserId
                )
                .orElseThrow(() ->
                        new AccessDeniedException("Access denied")
                );

        // 2. Destination account must exist
        Account destinationAccount = accountRepository
                .findById(request.toAccountId())
                .orElseThrow(() ->
        new ResourceNotFoundException("Destination account not found"));

        // 3. Source and destination cannot be the same
        if (sourceAccount.getId()
                .equals(destinationAccount.getId())) {

            throw new InvalidTransferException(
        "Source and destination accounts must be different");
        }

        // 4. Source account must have enough balance
        if (sourceAccount.getBalance()
                .compareTo(request.amount()) < 0) {

            throw new InsufficientBalanceException(
        "Insufficient balance");
        }

        // 5. Debit source account ONCE
        sourceAccount.setBalance(
                sourceAccount.getBalance()
                        .subtract(request.amount())
        );

        // 6. Credit destination account ONCE
        destinationAccount.setBalance(
                destinationAccount.getBalance()
                        .add(request.amount())
        );

        // 7. Create transfer record
        Transfer transfer = new Transfer(
                sourceAccount.getId(),
                destinationAccount.getId(),
                request.amount()
        );

        transferRepository.save(transfer);

        // 8. TEST-ONLY FAILURE INJECTION
        //
        // At this point:
        //
        // Source account  -> debited
        // Destination     -> credited
        // Transfer        -> inserted
        //
        // Then we deliberately fail.
        //
        // Because this method is @Transactional,
        // the entire transaction should roll back.
        if (failAfterTransfer) {
            throw new RuntimeException(
                    "Simulated transfer failure"
            );
        }
    }
}