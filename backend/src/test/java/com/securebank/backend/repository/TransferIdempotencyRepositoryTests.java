package com.securebank.backend.repository;

import com.securebank.backend.entity.TransferIdempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TransferIdempotencyRepositoryTests {

    @Autowired
    private TransferIdempotencyRepository repository;

    


    @BeforeEach
    void resetDatabase() {
        repository.deleteAll();
    }

    @Test
    void canSaveAndFindByIdempotencyKey() {

        TransferIdempotency record =
                new TransferIdempotency(
                        "test-key-001",
                        5L,
                        "test-request-hash"
                );

        repository.save(record);

        var result =
                repository.findByIdempotencyKey("test-key-001");

        assertTrue(result.isPresent());

        assertEquals(
                "test-key-001",
                result.get().getIdempotencyKey()
        );

        assertEquals(
                5L,
                result.get().getUserId()
        );

        assertEquals(
                "test-request-hash",
                result.get().getRequestHash()
        );

        assertEquals(
                "PENDING",
                result.get().getStatus()
        );
    }

    @Test
void tryCreateClaimsKeyOnlyOnce() {

    int firstInsert =
            repository.tryCreate(
                    "atomic-key-001",
                    5L,
                    "hash-001"
            );

    int secondInsert =
            repository.tryCreate(
                    "atomic-key-001",
                    5L,
                    "hash-002"
            );

    assertEquals(
            1,
            firstInsert
    );

    assertEquals(
            0,
            secondInsert
    );

    var result =
            repository.findByIdempotencyKey(
                    "atomic-key-001"
            );

    assertTrue(result.isPresent());

    assertEquals(
            5L,
            result.get().getUserId()
    );

    assertEquals(
            "hash-001",
            result.get().getRequestHash()
    );

    assertEquals(
            "PENDING",
            result.get().getStatus()
    );
}
}