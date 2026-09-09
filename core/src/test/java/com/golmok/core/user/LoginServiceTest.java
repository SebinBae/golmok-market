package com.golmok.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LoginServiceTest {

    private static final String EMAIL = "gildong@example.com";
    private static final String PASSWORD = "password123";

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.create("길동", "01088887777", Instant.now()));
        userCredentialRepository.save(UserCredential.createLocal(
                user, EMAIL, passwordEncoder.encode(PASSWORD), Instant.now()));
        userId = user.getId();
    }

    @Test
    void 자격_증명이_맞으면_사용자를_알려준다() {
        assertThat(loginService.authenticate(EMAIL, PASSWORD)).isEqualTo(userId);
    }

    @Test
    void 비밀번호가_틀리면_거부한다() {
        assertThatThrownBy(() -> loginService.authenticate(EMAIL, "wrongpassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void 없는_이메일도_똑같은_메시지로_거부한다() {
        assertThatThrownBy(() -> loginService.authenticate("nobody@example.com", PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
