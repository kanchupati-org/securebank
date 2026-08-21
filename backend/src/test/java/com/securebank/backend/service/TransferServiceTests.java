package com.securebank.backend.service;

import com.securebank.backend.dto.TransferRequest;
import com.securebank.backend.entity.Account;
import com.securebank.backend.exception.AccessDeniedException;
import com.securebank.backend.exception.InvalidTransferException;
import com.securebank.backend.exception.IdempotencyConflictException;
import com.securebank.backend.repository.AccountRepository;
import com.securebank.backend.repository.TransferRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.securebank.backend.repository.TransferIdempotencyRepository;




import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@SpringBootTest
@ActiveProfiles("test")
class TransferServiceTests {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TransferIdempotencyRepository transferIdempotencyRepository;



    @BeforeEach
    void resetDatabase() {

        Account source = accountRepository.findById(1L)
                .orElseThrow();

        Account destination = accountRepository.findById(2L)
                .orElseThrow();

        source.setBalance(new BigDecimal("10000.00"));
        destination.setBalance(new BigDecimal("25000.00"));

        accountRepository.save(source);
        accountRepository.save(destination);

        transferIdempotencyRepository.deleteAll();
        transferRepository.deleteAll();

        transferService.setFailAfterTransfer(false);
    }


    

    // tests...
    @Test
    void customerCannotAuthorizeTransferFromAnotherUsersAccount() {

        TransferRequest request = new TransferRequest(
                2L,
                1L,
                new BigDecimal("1000.00")
        );

        assertThrows(
                AccessDeniedException.class,
                () -> transferService.transfer(
                        request,
                        5L
                )
        );
    }

    @Test
void customerCanTransferMoneyFromOwnAccount() {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    transferService.transfer(
            request,
            5L
    );

    Account source = accountRepository.findById(1L)
            .orElseThrow();

    Account destination = accountRepository.findById(2L)
            .orElseThrow();

    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destination.getBalance()
    );
}

@Test
void transferRollsBackWhenFailureOccursAfterTransferRecord() {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    transferService.setFailAfterTransfer(true);

    assertThrows(
            RuntimeException.class,
            () -> transferService.transfer(request, 5L)
    );

    Account source = accountRepository.findById(1L)
            .orElseThrow();

    Account destination = accountRepository.findById(2L)
            .orElseThrow();

    assertEquals(
            new BigDecimal("10000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("25000.00"),
            destination.getBalance()
    );

    assertEquals(
            0,
            transferRepository.count()
    );

    transferService.setFailAfterTransfer(false);
}

@Test
void transferDoesNotCurrentlyUsePessimisticLock() {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    transferService.transfer(
            request,
            5L
    );

    Account source = accountRepository.findById(1L)
            .orElseThrow();

    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );
}

@Test
void concurrentTransfersFromSameAccount() throws Exception {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("8000.00")
    );

    ExecutorService executor = Executors.newFixedThreadPool(2);

    CountDownLatch start = new CountDownLatch(1);

    Future<?> transfer1 = executor.submit(() -> {
        try {
            start.await();
            transferService.transfer(request, 5L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    Future<?> transfer2 = executor.submit(() -> {
        try {
            start.await();
            transferService.transfer(request, 5L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    // Release both threads at approximately the same time.
    start.countDown();

    Exception exception1 = null;
    Exception exception2 = null;

    try {
        transfer1.get();
    } catch (Exception e) {
        exception1 = e;
    }

    try {
        transfer2.get();
    } catch (Exception e) {
        exception2 = e;
    }

    executor.shutdown();

    Account source = accountRepository.findById(1L)
            .orElseThrow();

    Account destination = accountRepository.findById(2L)
            .orElseThrow();

    int successfulTransfers =
            (exception1 == null ? 1 : 0)
            + (exception2 == null ? 1 : 0);

    System.out.println("Transfer 1 exception = " + exception1);
    System.out.println("Transfer 2 exception = " + exception2);

    System.out.println(
            "Source balance = " + source.getBalance()
    );

    System.out.println(
            "Destination balance = " + destination.getBalance()
    );

    System.out.println(
            "Transfer count = " + transferRepository.count()
    );

    assertEquals(
            new BigDecimal("2000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("33000.00"),
            destination.getBalance()
    );

    assertEquals(
            1,
            transferRepository.count()
    );

    assertEquals(
            1,
            successfulTransfers
    );
}


@Test
void concurrentTransfersInOppositeDirections() throws Exception {

    TransferRequest transferAtoB = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    TransferRequest transferBtoA = new TransferRequest(
            2L,
            1L,
            new BigDecimal("1000.00")
    );

    ExecutorService executor =
            Executors.newFixedThreadPool(2);

    CountDownLatch start =
            new CountDownLatch(1);

    Future<?> transfer1 = executor.submit(() -> {
        try {
            start.await();

            transferService.transfer(
                    transferAtoB,
                    5L
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    Future<?> transfer2 = executor.submit(() -> {
        try {
            start.await();

            transferService.transfer(
                    transferBtoA,
                    7L
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    // Start both transactions at approximately the same time.
    start.countDown();

    Exception exception1 = null;
    Exception exception2 = null;

    try {
        transfer1.get();
    } catch (Exception e) {
        exception1 = e;
    }

    try {
        transfer2.get();
    } catch (Exception e) {
        exception2 = e;
    }

    executor.shutdown();

    Account account1 = accountRepository.findById(1L)
            .orElseThrow();

    Account account2 = accountRepository.findById(2L)
            .orElseThrow();

    int successfulTransfers =
            (exception1 == null ? 1 : 0)
            + (exception2 == null ? 1 : 0);

    System.out.println(
            "Transfer 1 exception = " + exception1
    );

    System.out.println(
            "Transfer 2 exception = " + exception2
    );

    System.out.println(
            "Account 1 balance = " + account1.getBalance()
    );

    System.out.println(
            "Account 2 balance = " + account2.getBalance()
    );

    System.out.println(
            "Transfer count = " + transferRepository.count()
    );

    /*
     * The two transfers move the same amount
     * in opposite directions.
     *
     * Account 1:
     * 10000 - 1000 + 1000 = 10000
     *
     * Account 2:
     * 25000 + 1000 - 1000 = 25000
     */
    assertEquals(
            new BigDecimal("10000.00"),
            account1.getBalance()
    );

    assertEquals(
            new BigDecimal("25000.00"),
            account2.getBalance()
    );

    /*
     * Both transfers should succeed.
     */
    assertEquals(
            2,
            successfulTransfers
    );

    /*
     * Both transfers should have a database record.
     */
    assertEquals(
            2,
            transferRepository.count()
    );
}

@Test
void sameIdempotencyKeyMustNotExecuteTransferTwice() {

    String idempotencyKey = "transfer-test-001";

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    transferService.transfer(
            request,
            5L,
            idempotencyKey
    );

    transferService.transfer(
            request,
            5L,
            idempotencyKey
    );

    Account source = accountRepository.findById(1L)
            .orElseThrow();

    Account destination = accountRepository.findById(2L)
            .orElseThrow();

    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destination.getBalance()
    );

    assertEquals(
            1,
            transferRepository.count()
    );

    assertEquals(
            1,
            transferIdempotencyRepository.count()
    );
}

@Test
void sameIdempotencyKeyWithDifferentRequestMustBeRejected() {

    TransferRequest firstRequest =
            new TransferRequest(
                    1L,
                    2L,
                    new BigDecimal("1000.00")
            );

    TransferRequest differentRequest =
            new TransferRequest(
                    1L,
                    2L,
                    new BigDecimal("2000.00")
            );

    String idempotencyKey = "transfer-test-002";

    transferService.transfer(
            firstRequest,
            5L,
            idempotencyKey
    );

    assertThrows(
            IdempotencyConflictException.class,
            () -> transferService.transfer(
                    differentRequest,
                    5L,
                    idempotencyKey
            )
    );

    Account source =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destination =
            accountRepository.findById(2L)
                    .orElseThrow();

    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destination.getBalance()
    );

    assertEquals(
            1,
            transferRepository.count()
    );
}

@Test
void differentUserCannotReuseAnotherUsersIdempotencyKey() {

    TransferRequest request =
            new TransferRequest(
                    1L,
                    2L,
                    new BigDecimal("1000.00")
            );

    String idempotencyKey = "transfer-test-003";

    // User 5 creates the idempotency key.
    transferService.transfer(
            request,
            5L,
            idempotencyKey
    );

    // User 7 tries to reuse User 5's key.
    assertThrows(
            AccessDeniedException.class,
            () -> transferService.transfer(
                    request,
                    7L,
                    idempotencyKey
            )
    );

    Account source =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destination =
            accountRepository.findById(2L)
                    .orElseThrow();

    // Original transfer happened exactly once.
    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destination.getBalance()
    );

    assertEquals(
            1,
            transferRepository.count()
    );

    assertEquals(
            1,
            transferIdempotencyRepository.count()
    );
}

@Test
void concurrentRequestsWithSameIdempotencyKeyMustExecuteOnlyOnce()
        throws Exception {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    String idempotencyKey = "concurrent-transfer-001";

    ExecutorService executor =
            Executors.newFixedThreadPool(2);

    CountDownLatch start =
            new CountDownLatch(1);

    Future<?> request1 = executor.submit(() -> {
        try {
            start.await();

            transferService.transfer(
                    request,
                    5L,
                    idempotencyKey
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    Future<?> request2 = executor.submit(() -> {
        try {
            start.await();

            transferService.transfer(
                    request,
                    5L,
                    idempotencyKey
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    // Start both requests.
    start.countDown();

    Exception exception1 = null;
    Exception exception2 = null;

    try {
        request1.get();
    } catch (Exception e) {
        exception1 = e;
    }

    try {
        request2.get();
    } catch (Exception e) {
        exception2 = e;
    }

    executor.shutdown();

    /*
     * BOTH concurrent requests should complete successfully.
     *
     * One request performs the transfer.
     * The other request recognizes the same idempotency key
     * and must not execute the transfer again.
     */
    assertEquals(
            null,
            exception1
    );

    assertEquals(
            null,
            exception2
    );

    Account source =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destination =
            accountRepository.findById(2L)
                    .orElseThrow();

    System.out.println(
            "Request 1 exception = " + exception1
    );

    System.out.println(
            "Request 2 exception = " + exception2
    );

    System.out.println(
            "Source balance = " + source.getBalance()
    );

    System.out.println(
            "Destination balance = " + destination.getBalance()
    );

    System.out.println(
            "Transfer count = " + transferRepository.count()
    );

    System.out.println(
            "Idempotency count = "
                    + transferIdempotencyRepository.count()
    );

    /*
     * The financial operation must happen only once.
     */
    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destination.getBalance()
    );

    assertEquals(
            1,
            transferRepository.count()
    );

    assertEquals(
            1,
            transferIdempotencyRepository.count()
    );

     // executor.shutdown();
}

@Test
void sameIdempotencyKeyWithDifferentRequestMustFail()
        throws Exception {

    String idempotencyKey = "same-key-different-request";

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
                    new BigDecimal("2000.00")
            );

    // First request succeeds.
    transferService.transfer(
            firstRequest,
            5L,
            idempotencyKey
    );

    // Same key, but different amount.
    assertThrows(
            IdempotencyConflictException.class,
            () -> transferService.transfer(
                    secondRequest,
                    5L,
                    idempotencyKey
            )
    );

    Account source =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destination =
            accountRepository.findById(2L)
                    .orElseThrow();

    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destination.getBalance()
    );

    assertEquals(
            1,
            transferRepository.count()
    );

    assertEquals(
            1,
            transferIdempotencyRepository.count()
    );
}

@Test
void differentUserCannotReuseIdempotencyKey()
        throws Exception {

    String idempotencyKey = "user-bound-key-001";

    TransferRequest request =
            new TransferRequest(
                    1L,
                    2L,
                    new BigDecimal("1000.00")
            );

    // User 5 owns account 1.
    transferService.transfer(
            request,
            5L,
            idempotencyKey
    );

    // User 7 attempts to reuse the same idempotency key.
    assertThrows(
            AccessDeniedException.class,
            () -> transferService.transfer(
                    request,
                    7L,
                    idempotencyKey
            )
    );

    Account source =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destination =
            accountRepository.findById(2L)
                    .orElseThrow();

    // Only the first transfer happened.
    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destination.getBalance()
    );

    assertEquals(
            1,
            transferRepository.count()
    );

    assertEquals(
            1,
            transferIdempotencyRepository.count()
    );
}

@Test
void failedTransferMustRollbackIdempotencyRecord()
        throws Exception {

    String idempotencyKey =
            "rollback-idempotency-001";

    TransferRequest request =
            new TransferRequest(
                    1L,
                    2L,
                    new BigDecimal("1000.00")
            );

    transferService.setFailAfterTransfer(true);

    assertThrows(
            RuntimeException.class,
            () -> transferService.transfer(
                    request,
                    5L,
                    idempotencyKey
            )
    );

    transferService.setFailAfterTransfer(false);

    Account source =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destination =
            accountRepository.findById(2L)
                    .orElseThrow();

    assertEquals(
            new BigDecimal("10000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("25000.00"),
            destination.getBalance()
    );

    assertEquals(
            0,
            transferRepository.count()
    );

    assertEquals(
            0,
            transferIdempotencyRepository.count()
    );
}

@Test
void successfulIdempotentTransferIsMarkedCompleted() {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    String idempotencyKey = "completed-status-001";

    transferService.transfer(
            request,
            5L,
            idempotencyKey
    );

    var record =
            transferIdempotencyRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

    var transfer =
            transferRepository.findAll()
                    .stream()
                    .findFirst()
                    .orElseThrow();

    assertEquals(
            "COMPLETED",
            record.getStatus()
    );

    assertEquals(
            transfer.getId(),
            record.getTransferId()
    );

    assertEquals(
            1,
            transferRepository.count()
    );
}


@Test
void completedIdempotentRequestMustNotExecuteTransferAgain()
        throws Exception {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    String idempotencyKey =
            "completed-replay-001";

    // First request executes the transfer.
    transferService.transfer(
            request,
            5L,
            idempotencyKey
    );

    // Capture the state after the successful transfer.
    Account sourceAfterFirst =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destinationAfterFirst =
            accountRepository.findById(2L)
                    .orElseThrow();

    var firstRecord =
            transferIdempotencyRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

    Long firstTransferId =
            firstRecord.getTransferId();

    // Same request + same key = replay.
    transferService.transfer(
            request,
            5L,
            idempotencyKey
    );

    Account sourceAfterReplay =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destinationAfterReplay =
            accountRepository.findById(2L)
                    .orElseThrow();

    var replayRecord =
            transferIdempotencyRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

    assertEquals(
            new BigDecimal("9000.00"),
            sourceAfterFirst.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destinationAfterFirst.getBalance()
    );

    // Replay must not debit/credit again.
    assertEquals(
            sourceAfterFirst.getBalance(),
            sourceAfterReplay.getBalance()
    );

    assertEquals(
            destinationAfterFirst.getBalance(),
            destinationAfterReplay.getBalance()
    );

    // Only one transfer must exist.
    assertEquals(
            1,
            transferRepository.count()
    );

    // The same completed transfer must remain associated
    // with the idempotency key.
    assertEquals(
            "COMPLETED",
            replayRecord.getStatus()
    );

    assertEquals(
            firstTransferId,
            replayRecord.getTransferId()
    );
}

@Test
void failedIdempotentTransferMustRollbackIdempotencyRecord()
        throws Exception {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    String idempotencyKey = "rollback-idempotency-001";

    transferService.setFailAfterTransfer(true);

    assertThrows(
            RuntimeException.class,
            () -> transferService.transfer(
                    request,
                    5L,
                    idempotencyKey
            )
    );

    assertEquals(
            0,
            transferRepository.count()
    );

    assertEquals(
            0,
            transferIdempotencyRepository.count()
    );

    Account source =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destination =
            accountRepository.findById(2L)
                    .orElseThrow();

    assertEquals(
            new BigDecimal("10000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("25000.00"),
            destination.getBalance()
    );

    transferService.setFailAfterTransfer(false);
}
@Test
void failedTransferCanBeRetriedWithSameIdempotencyKey()
        throws Exception {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    String idempotencyKey = "retry-after-failure-001";

    // First attempt fails.
    transferService.setFailAfterTransfer(true);

    assertThrows(
            RuntimeException.class,
            () -> transferService.transfer(
                    request,
                    5L,
                    idempotencyKey
            )
    );

    // The failed transaction must have rolled back completely.
    assertEquals(
            0,
            transferRepository.count()
    );

    assertEquals(
            0,
            transferIdempotencyRepository.count()
    );

    // Second attempt with the SAME key should be allowed.
    transferService.setFailAfterTransfer(false);

    transferService.transfer(
            request,
            5L,
            idempotencyKey
    );

    Account source =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destination =
            accountRepository.findById(2L)
                    .orElseThrow();

    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destination.getBalance()
    );

    assertEquals(
            1,
            transferRepository.count()
    );

    var record =
            transferIdempotencyRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

    assertEquals(
            "COMPLETED",
            record.getStatus()
    );
}
@Test
void concurrentRequestsWithSameIdempotencyKeyExecuteTransferOnlyOnce()
        throws Exception {

    TransferRequest request = new TransferRequest(
            1L,
            2L,
            new BigDecimal("1000.00")
    );

    String idempotencyKey = "concurrent-same-key-001";

    ExecutorService executor =
            Executors.newFixedThreadPool(2);

    CountDownLatch start =
            new CountDownLatch(1);

    Future<?> request1 = executor.submit(() -> {
        try {
            start.await();

            transferService.transfer(
                    request,
                    5L,
                    idempotencyKey
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    Future<?> request2 = executor.submit(() -> {
        try {
            start.await();

            transferService.transfer(
                    request,
                    5L,
                    idempotencyKey
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    // Release both requests at approximately the same time.
    start.countDown();

    Exception exception1 = null;
    Exception exception2 = null;

    try {
        request1.get();
    } catch (Exception e) {
        exception1 = e;
    }

    try {
        request2.get();
    } catch (Exception e) {
        exception2 = e;
    }

    executor.shutdown();

    Account source =
            accountRepository.findById(1L)
                    .orElseThrow();

    Account destination =
            accountRepository.findById(2L)
                    .orElseThrow();

    var idempotencyRecord =
            transferIdempotencyRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

    System.out.println(
            "Request 1 exception = " + exception1
    );

    System.out.println(
            "Request 2 exception = " + exception2
    );

    System.out.println(
            "Source balance = " + source.getBalance()
    );

    System.out.println(
            "Destination balance = " + destination.getBalance()
    );

    System.out.println(
            "Transfer count = " + transferRepository.count()
    );

    System.out.println(
            "Idempotency count = "
                    + transferIdempotencyRepository.count()
    );

    assertEquals(
            new BigDecimal("9000.00"),
            source.getBalance()
    );

    assertEquals(
            new BigDecimal("26000.00"),
            destination.getBalance()
    );

    assertEquals(
            1,
            transferRepository.count()
    );

    assertEquals(
            1,
            transferIdempotencyRepository.count()
    );

    assertEquals(
            "COMPLETED",
            idempotencyRecord.getStatus()
    );
}

}
