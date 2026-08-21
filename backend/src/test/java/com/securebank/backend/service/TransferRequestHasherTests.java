package com.securebank.backend.service;

import com.securebank.backend.dto.TransferRequest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TransferRequestHasherTests {

    private final TransferRequestHasher hasher =
            new TransferRequestHasher();

    @Test
    void sameRequestProducesSameHash() {

        TransferRequest request1 =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        TransferRequest request2 =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        String hash1 =
                hasher.hash(request1, 5L);

        String hash2 =
                hasher.hash(request2, 5L);

        assertEquals(hash1, hash2);
    }

    @Test
    void differentAmountProducesDifferentHash() {

        TransferRequest request1 =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        TransferRequest request2 =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("900.00")
                );

        String hash1 =
                hasher.hash(request1, 5L);

        String hash2 =
                hasher.hash(request2, 5L);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void differentSourceAccountProducesDifferentHash() {

        TransferRequest request1 =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        TransferRequest request2 =
                new TransferRequest(
                        2L,
                        1L,
                        new BigDecimal("1000.00")
                );

        String hash1 =
                hasher.hash(request1, 5L);

        String hash2 =
                hasher.hash(request2, 5L);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void differentUserProducesDifferentHash() {

        TransferRequest request =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        String hash1 =
                hasher.hash(request, 5L);

        String hash2 =
                hasher.hash(request, 7L);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void equivalentMoneyFormattingProducesSameHash() {

        TransferRequest request1 =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000")
                );

        TransferRequest request2 =
                new TransferRequest(
                        1L,
                        2L,
                        new BigDecimal("1000.00")
                );

        String hash1 =
                hasher.hash(request1, 5L);

        String hash2 =
                hasher.hash(request2, 5L);

        assertEquals(hash1, hash2);
    }
}