package com.golmok.core.user;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** 측정용. 단언하지 않는다 (시간 기반 단언은 불안정하다). */
@SpringBootTest
@Transactional
class LoginTimingMeasurement {

    private static final String EMAIL = "gildong@example.com";

    @Autowired
    private LoginService loginService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCredentialRepository userCredentialRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.create("길동", "01088887777", Instant.now()));
        userCredentialRepository.save(UserCredential.createLocal(
                user, EMAIL, passwordEncoder.encode("password123"), Instant.now()));
    }

    private long medianMillis(String email) {
        long[] samples = new long[11];
        for (int i = 0; i < samples.length; i++) {
            long start = System.nanoTime();
            try {
                loginService.authenticate(email, "wrongpassword");
            } catch (IllegalArgumentException ignored) {
                // 실패 경로만 잰다
            }
            samples[i] = (System.nanoTime() - start) / 1_000_000;
        }
        java.util.Arrays.sort(samples);
        return samples[samples.length / 2];
    }

    @Test
    void 존재하는_이메일과_없는_이메일의_응답_시간() {
        medianMillis(EMAIL); // 워밍업

        long existing = medianMillis(EMAIL);
        long missing = medianMillis("nobody@example.com");

        System.out.printf("MEASURE existing=%dms missing=%dms%n", existing, missing);
    }
}
