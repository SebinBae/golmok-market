package com.golmok.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.golmok.core.sms.SmsSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SignUpServiceTest {

    private static final String PHONE = "01077776666";
    private static final String EMAIL = "gildong@example.com";
    private static final String PASSWORD = "password123";
    private static final String NICKNAME = "홍길동";

    @Autowired
    private SignUpService signUpService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @MockitoBean
    private SmsSender smsSender;

    /** A3을 실제로 통과시킨다. 코드는 SMS로만 나가므로 발송 메시지에서 뽑는다. */
    private void passPhoneVerification(String phoneNumber) {
        verificationCodeService.issue(phoneNumber, VerificationPurpose.SIGN_UP);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(smsSender).send(eq(phoneNumber), message.capture());
        verificationCodeService.verify(phoneNumber, VerificationPurpose.SIGN_UP,
                message.getValue().replaceAll("\\D", ""));
    }

    @Test
    void 인증을_마치면_User와_UserCredential이_함께_생긴다() {
        passPhoneVerification(PHONE);

        Long userId = signUpService.signUp(EMAIL, PASSWORD, PHONE, NICKNAME);

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getNickname()).isEqualTo(NICKNAME);
        assertThat(user.getPhoneNumber()).isEqualTo(PHONE);

        UserCredential credential = userCredentialRepository
                .findByProviderAndEmail(AuthProvider.LOCAL, EMAIL).orElseThrow();
        assertThat(credential.getUser().getId()).isEqualTo(userId);
        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getPasswordHash()).isNotEqualTo(PASSWORD);
    }

    @Test
    void 휴대폰_인증_없이는_가입할_수_없다() {
        assertThatThrownBy(() -> signUpService.signUp(EMAIL, PASSWORD, PHONE, NICKNAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("휴대폰 인증");
    }

    @Test
    void 다른_번호로_인증해도_가입할_수_없다() {
        passPhoneVerification("01055554444");

        assertThatThrownBy(() -> signUpService.signUp(EMAIL, PASSWORD, PHONE, NICKNAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("휴대폰 인증");
    }

    @Test
    void 같은_이메일로_두_번_가입할_수_없다() {
        passPhoneVerification(PHONE);
        signUpService.signUp(EMAIL, PASSWORD, PHONE, NICKNAME);

        assertThatThrownBy(() -> signUpService.signUp(EMAIL, PASSWORD, PHONE, "다른닉네임"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 가입된 이메일");
    }

    @Test
    void 같은_번호로_두_번_가입할_수_없다() {
        passPhoneVerification(PHONE);
        signUpService.signUp(EMAIL, PASSWORD, PHONE, NICKNAME);

        assertThatThrownBy(() -> signUpService.signUp("other@example.com", PASSWORD, PHONE, "다른닉네임"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 가입된 휴대폰");
    }

    @Test
    void 짧은_비밀번호는_거부한다() {
        passPhoneVerification(PHONE);

        assertThatThrownBy(() -> signUpService.signUp(EMAIL, "short", PHONE, NICKNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8자 이상");
    }

    @Test
    void 한_글자_닉네임은_거부한다() {
        passPhoneVerification(PHONE);

        assertThatThrownBy(() -> signUpService.signUp(EMAIL, PASSWORD, PHONE, "김"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2~20자");
    }
}
