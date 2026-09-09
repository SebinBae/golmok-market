package com.golmok.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.golmok.core.sms.SmsSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class VerificationCodeServiceTest {

    private static final String PHONE = "01099998888";

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @MockitoBean
    private SmsSender smsSender;

    @AfterEach
    void tearDown() {
        verificationCodeRepository.deleteAll();
    }

    private String issueAndCaptureCode(VerificationPurpose purpose) {
        verificationCodeService.issue(PHONE, purpose);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(smsSender).send(eq(PHONE), message.capture());
        return message.getValue().replaceAll("\\D", "");
    }

    @Test
    @Transactional
    void 발급한_코드로_검증에_성공한다() {
        String code = issueAndCaptureCode(VerificationPurpose.SIGN_UP);

        assertThat(code).hasSize(6);
        verificationCodeService.verify(PHONE, VerificationPurpose.SIGN_UP, code);

        assertThat(verificationCodeRepository
                .findFirstByPhoneNumberAndPurposeOrderByCreatedAtDesc(PHONE, VerificationPurpose.SIGN_UP)
                .orElseThrow()
                .getUsedAt()).isNotNull();
    }

    @Test
    @Transactional
    void 일_분_내_재발급은_거부한다() {
        verificationCodeService.issue(PHONE, VerificationPurpose.SIGN_UP);

        assertThatThrownBy(() -> verificationCodeService.issue(PHONE, VerificationPurpose.SIGN_UP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1분에 한 번");
    }

    /** 트랜잭션을 일부러 걸지 않는다. 커밋까지 가야 시도 횟수가 실제로 남는지 알 수 있다. */
    @Test
    void 틀린_코드는_거부하고_시도_횟수를_남긴다() {
        issueAndCaptureCode(VerificationPurpose.SIGN_UP);

        assertThatThrownBy(() -> verificationCodeService.verify(PHONE, VerificationPurpose.SIGN_UP, "000000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("일치하지 않");

        assertThat(verificationCodeRepository
                .findFirstByPhoneNumberAndPurposeOrderByCreatedAtDesc(PHONE, VerificationPurpose.SIGN_UP)
                .orElseThrow()
                .getAttemptCount()).isEqualTo(1);
    }

    @Test
    @Transactional
    void 시도_횟수를_초과하면_더_시도할_수_없다() {
        String code = issueAndCaptureCode(VerificationPurpose.SIGN_UP);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> verificationCodeService.verify(PHONE, VerificationPurpose.SIGN_UP, "000000"))
                    .hasMessageContaining("일치하지 않");
        }

        assertThatThrownBy(() -> verificationCodeService.verify(PHONE, VerificationPurpose.SIGN_UP, code))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("시도 횟수");
    }

    @Test
    @Transactional
    void 사용한_코드는_다시_쓸_수_없다() {
        String code = issueAndCaptureCode(VerificationPurpose.SIGN_UP);
        verificationCodeService.verify(PHONE, VerificationPurpose.SIGN_UP, code);

        assertThatThrownBy(() -> verificationCodeService.verify(PHONE, VerificationPurpose.SIGN_UP, code))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용");
    }

    @Test
    @Transactional
    void 다른_용도의_코드로는_검증되지_않는다() {
        String code = issueAndCaptureCode(VerificationPurpose.SIGN_UP);

        assertThatThrownBy(() -> verificationCodeService.verify(PHONE, VerificationPurpose.FIND_EMAIL, code))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("발급된 인증번호가 없");
    }
}
