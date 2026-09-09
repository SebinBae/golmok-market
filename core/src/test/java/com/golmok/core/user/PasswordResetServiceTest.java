package com.golmok.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.golmok.core.mail.MailSender;
import com.golmok.core.sms.SmsSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PasswordResetServiceTest {

    private static final String EMAIL = "gildong@example.com";
    private static final String OLD_PASSWORD = "oldpassword1";
    private static final String NEW_PASSWORD = "newpassword1";

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MailSender mailSender;

    @MockitoBean
    private SmsSender smsSender;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.create("길동", "01088887777", Instant.now()));
        userCredentialRepository.save(UserCredential.createLocal(
                user, EMAIL, passwordEncoder.encode(OLD_PASSWORD), Instant.now()));
    }

    private String requestAndCaptureToken() {
        passwordResetService.requestReset(EMAIL);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailSender, atLeastOnce()).send(eq(EMAIL), any(), body.capture());
        return body.getValue().replaceAll("^.*token=", "");
    }

    private String currentPasswordHash() {
        return userCredentialRepository.findByProviderAndEmail(AuthProvider.LOCAL, EMAIL)
                .orElseThrow().getPasswordHash();
    }

    @Test
    void 링크로_비밀번호를_바꾼다() {
        String token = requestAndCaptureToken();

        passwordResetService.resetPassword(token, NEW_PASSWORD);

        assertThat(passwordEncoder.matches(NEW_PASSWORD, currentPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(OLD_PASSWORD, currentPasswordHash())).isFalse();
    }

    @Test
    void 같은_링크를_두_번_쓸_수_없다() {
        String token = requestAndCaptureToken();
        passwordResetService.resetPassword(token, NEW_PASSWORD);

        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "anotherpass1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용");
    }

    @Test
    void 새_링크를_발급하면_이전_링크는_무효가_된다() {
        String oldToken = requestAndCaptureToken();
        String newToken = requestAndCaptureToken();

        assertThat(newToken).isNotEqualTo(oldToken);
        assertThatThrownBy(() -> passwordResetService.resetPassword(oldToken, NEW_PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("유효하지 않은");

        passwordResetService.resetPassword(newToken, NEW_PASSWORD);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, currentPasswordHash())).isTrue();
    }

    @Test
    void 가입하지_않은_이메일은_조용히_넘어간다() {
        passwordResetService.requestReset("nobody@example.com");

        verify(mailSender, never()).send(eq("nobody@example.com"), any(), any());
    }

    @Test
    void 아무_토큰이나_통하지_않는다() {
        requestAndCaptureToken();

        assertThatThrownBy(() -> passwordResetService.resetPassword("made-up-token", NEW_PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    void 짧은_비밀번호로는_바꿀_수_없다() {
        String token = requestAndCaptureToken();

        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8자 이상");
    }

    @Test
    void DB에는_평문_토큰이_아니라_SHA_256_해시가_저장된다() throws Exception {
        String token = requestAndCaptureToken();

        String stored = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM password_reset_token", String.class);

        assertThat(stored).isNotEqualTo(token);
        assertThat(stored).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(stored).isEqualTo(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))));
    }
}
