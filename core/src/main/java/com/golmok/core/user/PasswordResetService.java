package com.golmok.core.user;

import com.golmok.core.mail.MailSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetService {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int TOKEN_BYTES = 32;
    private static final int PASSWORD_MIN_LENGTH = 8;

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserCredentialRepository userCredentialRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                PasswordEncoder passwordEncoder,
                                MailSender mailSender) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    /**
     * 가입되지 않은 이메일이어도 조용히 끝난다 (screens.md A6, 열거 방지).
     * 호출자는 성공·실패를 구분할 수 없고, 구분할 필요도 없다.
     */
    public void requestReset(String email) {
        userCredentialRepository.findByProviderAndEmail(AuthProvider.LOCAL, email)
                .ifPresent(this::issueToken);
    }

    public void resetPassword(String rawToken, String newRawPassword) {
        if (newRawPassword == null || newRawPassword.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 %d자 이상이어야 합니다.".formatted(PASSWORD_MIN_LENGTH));
        }

        Instant now = Instant.now();
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalStateException("유효하지 않은 링크입니다."));

        if (token.isUsed()) {
            throw new IllegalStateException("이미 사용한 링크입니다.");
        }
        if (token.isExpired(now)) {
            throw new IllegalStateException("만료된 링크입니다.");
        }

        token.getCredential().changePassword(passwordEncoder.encode(newRawPassword));
        token.markUsed(now);
    }

    private void issueToken(UserCredential credential) {
        // 새로 발급하면 이전 미사용 토큰은 무효화한다. 살아있는 링크가 여러 개면
        // 유출된 옛 링크로도 비밀번호를 바꿀 수 있다.
        passwordResetTokenRepository.deleteByCredentialAndUsedAtIsNull(credential);

        Instant now = Instant.now();
        String rawToken = generateToken();
        passwordResetTokenRepository.save(
                PasswordResetToken.issue(credential, hash(rawToken), now, now.plus(TTL)));

        mailSender.send(credential.getEmail(), "[골목] 비밀번호 재설정",
                "아래 링크로 비밀번호를 재설정하세요. 30분 후 만료됩니다. token=" + rawToken);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256. 인증번호와 달리 BCrypt를 쓰지 않는다. 토큰이 랜덤 32바이트라
     * 전수 대입이 불가능해 느린 해시가 지켜줄 것이 없고, 링크를 열 때마다 비용만 든다.
     * 해시가 결정적이어야 token_hash로 바로 조회할 수 있다는 점도 있다.
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
