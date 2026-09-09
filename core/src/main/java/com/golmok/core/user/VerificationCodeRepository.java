package com.golmok.core.user;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findFirstByPhoneNumberAndPurposeOrderByCreatedAtDesc(
            String phoneNumber, VerificationPurpose purpose);

    boolean existsByPhoneNumberAndPurposeAndCreatedAtAfter(
            String phoneNumber, VerificationPurpose purpose, Instant createdAt);

    boolean existsByPhoneNumberAndPurposeAndUsedAtAfter(
            String phoneNumber, VerificationPurpose purpose, Instant usedAt);
}
