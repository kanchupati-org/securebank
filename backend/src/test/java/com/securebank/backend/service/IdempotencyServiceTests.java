package com.securebank.backend.service;

import com.securebank.backend.dto.TransferRequest;
import com.securebank.backend.entity.TransferIdempotency;
import com.securebank.backend.exception.AccessDeniedException;
import com.securebank.backend.exception.IdempotencyConflictException;
import com.securebank.backend.repository.TransferIdempotencyRepository;
import com.securebank.backend.repository.TransferRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.securebank.backend.entity.Transfer;
import com.securebank.backend.repository.TransferRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IdempotencyServiceTests {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private TransferIdempotencyRepository repository;

    @Autowired
    private TransferRepository transferRepository;

    @BeforeEach
    void resetDatabase() {
        repository.deleteAll();
    }

    @Test
    void newKeyCreatesPendingRecord() {

        TransferRequest request =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        TransferIdempotency record =
                idempotencyService.createOrGet(
                        "test-key-001",
                        5L,
                        request
                );

        assertNotNull(record.getId());

        assertEquals(
                "test-key-001",
                record.getIdempotencyKey()
        );

        assertEquals(
                5L,
                record.getUserId()
        );

        assertEquals(
                "PENDING",
                record.getStatus()
        );

        assertNull(record.getTransferId());

        assertEquals(
                1,
                repository.count()
        );
    }

    @Test
    void sameKeyAndSameRequestReturnsExistingRecord() {

        TransferRequest request =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        TransferIdempotency first =
                idempotencyService.createOrGet(
                        "test-key-002",
                        5L,
                        request
                );

        TransferIdempotency second =
                idempotencyService.createOrGet(
                        "test-key-002",
                        5L,
                        request
                );

        assertEquals(
                first.getId(),
                second.getId()
        );

        assertEquals(
                1,
                repository.count()
        );
    }

    @Test
    void sameKeyWithDifferentRequestIsRejected() {

        TransferRequest firstRequest =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        TransferRequest secondRequest =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("5000.00")
                );

        idempotencyService.createOrGet(
                "test-key-003",
                5L,
                firstRequest
        );

        assertThrows(
                IdempotencyConflictException.class,
                () -> idempotencyService.createOrGet(
                        "test-key-003",
                        5L,
                        secondRequest
                )
        );

        assertEquals(
                1,
                repository.count()
        );
    }

    @Test
    void sameKeyUsedByDifferentUserIsRejected() {

        TransferRequest request =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        idempotencyService.createOrGet(
                "test-key-004",
                5L,
                request
        );

        assertThrows(
                AccessDeniedException.class,
                () -> idempotencyService.createOrGet(
                        "test-key-004",
                        7L,
                        request
                )
        );

        assertEquals(
                1,
                repository.count()
        );
    }

    @Test
    void completedRecordCanBeMarkedCompleted() {

        TransferRequest request =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        TransferIdempotency record =
                idempotencyService.createOrGet(
                        "test-key-005",
                        5L,
                        request
                );

        Transfer transfer = new Transfer(
        1L,
        2L,
        new BigDecimal("1000.00")
);

transfer = transferRepository.saveAndFlush(transfer);

idempotencyService.markCompleted(
        record,
        transfer.getId()
);

        TransferIdempotency saved =
                repository
                        .findById(record.getId())
                        .orElseThrow();

        assertEquals(
                "COMPLETED",
                saved.getStatus()
        );

        assertEquals(
        transfer.getId(),
        saved.getTransferId()
);
    }
}
