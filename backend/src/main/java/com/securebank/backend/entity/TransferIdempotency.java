package com.securebank.backend.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "transfer_idempotency",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_transfer_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
public class TransferIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true
    )
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TransferIdempotency() {
    }

    public TransferIdempotency(
            String idempotencyKey,
            Long userId,
            String requestHash) {

        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.requestHash = requestHash;
        this.status = "PENDING";
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getStatus() {
        return status;
    }

    public Long getTransferId() {
        return transferId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void markCompleted(Long transferId) {
        this.status = "COMPLETED";
        this.transferId = transferId;
    }
}