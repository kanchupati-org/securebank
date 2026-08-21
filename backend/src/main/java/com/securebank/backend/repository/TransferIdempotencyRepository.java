package com.securebank.backend.repository;

import com.securebank.backend.entity.TransferIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface TransferIdempotencyRepository
        extends JpaRepository<TransferIdempotency, Long> {

    Optional<TransferIdempotency> findByIdempotencyKey(
            String idempotencyKey
    );

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO transfer_idempotency
                (
                    idempotency_key,
                    user_id,
                    request_hash,
                    status,
                    created_at
                )
            VALUES
                (
                    :idempotencyKey,
                    :userId,
                    :requestHash,
                    'PENDING',
                    CURRENT_TIMESTAMP
                )
            ON CONFLICT (idempotency_key)
            DO NOTHING
            """, nativeQuery = true)
    int tryCreate(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("userId") Long userId,
            @Param("requestHash") String requestHash
    );
}