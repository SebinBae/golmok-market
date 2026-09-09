package com.golmok.core.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** 새 토큰을 발급할 때 이전 미사용 토큰을 무효화한다. 사용된 토큰은 이력으로 남긴다. */
    void deleteByCredentialAndUsedAtIsNull(UserCredential credential);
}
