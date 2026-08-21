package com.securebank.backend.repository;

import com.securebank.backend.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT a
    FROM Account a
    WHERE a.id = :id
      AND a.userId = :userId
""")
Optional<Account> findByIdAndUserIdForUpdate(
        @Param("id") Long id,
        @Param("userId") Long userId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT a
    FROM Account a
    WHERE a.id IN (:firstId, :secondId)
    ORDER BY a.id
""")
List<Account> findTwoAccountsForUpdate(
        @Param("firstId") Long firstId,
        @Param("secondId") Long secondId
);
}