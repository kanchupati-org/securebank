package com.securebank.backend.service;

import com.securebank.backend.dto.TransferRequest;
import com.securebank.backend.entity.Account;
import com.securebank.backend.exception.AccessDeniedException;
import com.securebank.backend.repository.AccountRepository;
import com.securebank.backend.repository.TransferRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.securebank.backend.repository.TransferRepository;
import org.springframework.aop.support.AopUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Transactional
class TransferServiceTests {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;


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

        // transferRepository.deleteAll();

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
void transferServiceIsTransactionalProxy() {

    System.out.println(
            "TransferService class = "
                    + transferService.getClass()
    );

    System.out.println(
            "Is AOP proxy = "
                    + AopUtils.isAopProxy(transferService)
    );

    System.out.println(
            "Is CGLIB proxy = "
                    + AopUtils.isCglibProxy(transferService)
    );
}

@Test
void transferServiceIsTransactionalProxy() {

    System.out.println(
            "TransferService class = "
                    + transferService.getClass()
    );

    System.out.println(
            "Is AOP proxy = "
                    + AopUtils.isAopProxy(transferService)
    );

    System.out.println(
            "Is CGLIB proxy = "
                    + AopUtils.isCglibProxy(transferService)
    );

    System.out.println(
            "Transaction active before call = "
                    + TransactionSynchronizationManager
                            .isActualTransactionActive()
    );
}

    
}