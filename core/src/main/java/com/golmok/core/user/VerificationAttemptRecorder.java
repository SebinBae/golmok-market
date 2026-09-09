package com.golmok.core.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시도 횟수를 별도 트랜잭션에서 커밋한다.
 *
 * <p>호출자의 트랜잭션 안에서 올리면, 뒤이어 던지는 예외에 증가분이 함께 롤백된다.
 * 호출자마다 {@code noRollbackFor}를 반복해 거는 방법도 있지만 한 곳만 빠뜨려도
 * 아무 신호 없이 무차별 대입 방어가 사라진다. 여기서 한 번 보장한다.
 *
 * <p>자기 호출은 프록시를 타지 않으므로 별도 빈이어야 한다.
 */
@Component
public class VerificationAttemptRecorder {

    private final VerificationCodeRepository verificationCodeRepository;

    public VerificationAttemptRecorder(VerificationCodeRepository verificationCodeRepository) {
        this.verificationCodeRepository = verificationCodeRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(Long codeId) {
        verificationCodeRepository.findById(codeId).ifPresent(VerificationCode::recordFailedAttempt);
    }
}
