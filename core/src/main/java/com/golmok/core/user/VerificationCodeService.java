package com.golmok.core.user;

import com.golmok.core.sms.SmsSender;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VerificationCodeService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Duration REISSUE_INTERVAL = Duration.ofMinutes(1);
    private static final int CODE_BOUND = 1_000_000;

    private final VerificationCodeRepository verificationCodeRepository;
    private final VerificationAttemptRecorder attemptRecorder;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;
    private final SecureRandom random = new SecureRandom();

    public VerificationCodeService(VerificationCodeRepository verificationCodeRepository,
                                   VerificationAttemptRecorder attemptRecorder,
                                   PasswordEncoder passwordEncoder,
                                   SmsSender smsSender) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.attemptRecorder = attemptRecorder;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
    }

    public void issue(String phoneNumber, VerificationPurpose purpose) {
        Instant now = Instant.now();
        if (verificationCodeRepository.existsByPhoneNumberAndPurposeAndCreatedAtAfter(
                phoneNumber, purpose, now.minus(REISSUE_INTERVAL))) {
            throw new IllegalStateException("인증번호는 1분에 한 번만 요청할 수 있습니다.");
        }

        String rawCode = generateCode();
        verificationCodeRepository.save(VerificationCode.issue(
                phoneNumber, passwordEncoder.encode(rawCode), purpose, now, now.plus(TTL)));

        smsSender.send(phoneNumber, "[골목] 인증번호는 " + rawCode + "입니다.");
    }

    public void verify(String phoneNumber, VerificationPurpose purpose, String rawCode) {
        Instant now = Instant.now();
        VerificationCode code = verificationCodeRepository
                .findFirstByPhoneNumberAndPurposeOrderByCreatedAtDesc(phoneNumber, purpose)
                .orElseThrow(() -> new IllegalStateException("발급된 인증번호가 없습니다."));

        if (code.isUsed()) {
            throw new IllegalStateException("이미 사용한 인증번호입니다.");
        }
        if (code.isExpired(now)) {
            throw new IllegalStateException("인증번호가 만료되었습니다.");
        }
        if (code.isAttemptExceeded()) {
            throw new IllegalStateException("시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.");
        }
        if (!passwordEncoder.matches(rawCode, code.getCodeHash())) {
            attemptRecorder.recordFailedAttempt(code.getId());
            throw new IllegalStateException("인증번호가 일치하지 않습니다.");
        }

        code.markUsed(now);
    }

    private String generateCode() {
        return "%06d".formatted(random.nextInt(CODE_BOUND));
    }
}
