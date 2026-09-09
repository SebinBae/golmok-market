package com.golmok.core.user;

import java.time.Duration;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SignUpService {

    /** 휴대폰 인증(A3)을 통과한 뒤 닉네임(A4)까지 끝낼 수 있는 시간. */
    private static final Duration VERIFICATION_VALIDITY = Duration.ofMinutes(10);

    private static final int PASSWORD_MIN_LENGTH = 8;

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;

    public SignUpService(UserRepository userRepository,
                         UserCredentialRepository userCredentialRepository,
                         VerificationCodeRepository verificationCodeRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * A2~A4에서 모은 값을 한 번에 받는다. 단계를 어떻게 이어붙일지는 어댑터가 정한다
     * (web은 세션, api는 한 요청). core는 단계를 모른다.
     */
    public Long signUp(String email, String rawPassword, String phoneNumber, String nickname) {
        Instant now = Instant.now();

        requireVerifiedPhone(phoneNumber, now);

        if (rawPassword == null || rawPassword.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 %d자 이상이어야 합니다.".formatted(PASSWORD_MIN_LENGTH));
        }
        if (userCredentialRepository.existsByProviderAndEmail(AuthProvider.LOCAL, email)) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
        if (userRepository.existsByPhoneNumberAndDeletedAtIsNull(phoneNumber)) {
            throw new IllegalStateException("이미 가입된 휴대폰 번호입니다.");
        }
        if (userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        User user = userRepository.save(User.create(nickname, phoneNumber, now));
        userCredentialRepository.save(
                UserCredential.createLocal(user, email, passwordEncoder.encode(rawPassword), now));

        return user.getId();
    }

    /**
     * 어댑터를 믿지 않고 core가 직접 확인한다. web은 세션으로 A3 통과를 기억하지만
     * api는 그런 상태가 없어, core가 보지 않으면 아무 번호로나 가입할 수 있다.
     */
    private void requireVerifiedPhone(String phoneNumber, Instant now) {
        boolean verified = verificationCodeRepository.existsByPhoneNumberAndPurposeAndUsedAtAfter(
                phoneNumber, VerificationPurpose.SIGN_UP, now.minus(VERIFICATION_VALIDITY));
        if (!verified) {
            throw new IllegalStateException("휴대폰 인증이 필요합니다.");
        }
    }
}
