package com.golmok.core.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "verification_code")
public class VerificationCode {

    private static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 11)
    private String phoneNumber;

    @Column(nullable = false, length = 72)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationPurpose purpose;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private Instant createdAt;

    protected VerificationCode() {
    }

    public static VerificationCode issue(String phoneNumber, String codeHash, VerificationPurpose purpose,
                                         Instant createdAt, Instant expiresAt) {
        VerificationCode code = new VerificationCode();
        code.phoneNumber = phoneNumber;
        code.codeHash = codeHash;
        code.purpose = purpose;
        code.createdAt = createdAt;
        code.expiresAt = expiresAt;
        return code;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isAttemptExceeded() {
        return attemptCount >= MAX_ATTEMPTS;
    }

    public void recordFailedAttempt() {
        attemptCount++;
    }

    public void markUsed(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }
}
