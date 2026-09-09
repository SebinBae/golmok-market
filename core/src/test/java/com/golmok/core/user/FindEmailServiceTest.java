package com.golmok.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
class FindEmailServiceTest {

    private static final String PHONE = "01033332222";
    private static final String EMAIL = "hyeonjin@gmail.com";

    @Autowired
    private FindEmailService findEmailService;

    @Autowired
    private SignUpService signUpService;

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

    private String issueAndCapture(VerificationPurpose purpose) {
        verificationCodeService.issue(PHONE, purpose);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(smsSender, atLeastOnce()).send(eq(PHONE), message.capture());
        return message.getValue().replaceAll("\\D", "");
    }

    @Test
    @Transactional
    void 가입한_이메일을_마스킹해서_돌려준다() {
        String signUpCode = issueAndCapture(VerificationPurpose.SIGN_UP);
        verificationCodeService.verify(PHONE, VerificationPurpose.SIGN_UP, signUpCode);
        signUpService.signUp(EMAIL, "password123", PHONE, "현진");
        verificationCodeRepository.deleteAll();

        String findCode = issueAndCapture(VerificationPurpose.FIND_EMAIL);

        assertThat(findEmailService.findEmail(PHONE, findCode)).isEqualTo("hy****@gmail.com");
    }

    @Test
    @Transactional
    void 가입_내역이_없으면_그렇다고_알려준다() {
        String code = issueAndCapture(VerificationPurpose.FIND_EMAIL);

        assertThatThrownBy(() -> findEmailService.findEmail(PHONE, code))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("가입한 내역이 없");
    }

    @Test
    @Transactional
    void 인증번호가_틀리면_이메일을_알려주지_않는다() {
        issueAndCapture(VerificationPurpose.FIND_EMAIL);

        assertThatThrownBy(() -> findEmailService.findEmail(PHONE, "000000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("일치하지 않");
    }

    /** 커밋까지 가야 시도 횟수가 실제로 남는지 알 수 있다. */
    @Test
    void 틀린_인증번호_시도가_기록된다() {
        issueAndCapture(VerificationPurpose.FIND_EMAIL);

        assertThatThrownBy(() -> findEmailService.findEmail(PHONE, "000000"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(verificationCodeRepository
                .findFirstByPhoneNumberAndPurposeOrderByCreatedAtDesc(PHONE, VerificationPurpose.FIND_EMAIL)
                .orElseThrow()
                .getAttemptCount()).isEqualTo(1);
    }
}
