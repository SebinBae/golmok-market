package com.golmok.core.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false)
    private UserCredential credential;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected PasswordResetToken() {
    }

    public static PasswordResetToken issue(UserCredential credential, String tokenHash,
                                           Instant createdAt, Instant expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.credential = credential;
        token.tokenHash = tokenHash;
        token.createdAt = createdAt;
        token.expiresAt = expiresAt;
        return token;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void markUsed(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public UserCredential getCredential() {
        return credential;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
