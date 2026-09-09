package com.golmok.core.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * core가 로그인에서 할 수 있는 전부다. 자격 증명을 대조해 사용자를 알려준다.
 * 세션이냐 JWT냐, 로그아웃을 어떻게 하느냐는 어댑터가 정한다.
 */
@Service
@Transactional(readOnly = true)
public class LoginService {

    private static final String FAILURE_MESSAGE = "이메일 또는 비밀번호가 올바르지 않습니다.";

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    /** 없는 이메일일 때도 해싱 비용을 똑같이 치르기 위한 더미. 어떤 입력과도 일치하지 않는다. */
    private final String dummyHash;

    public LoginService(UserCredentialRepository userCredentialRepository,
                        PasswordEncoder passwordEncoder) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public Long authenticate(String email, String rawPassword) {
        Optional<UserCredential> credential =
                userCredentialRepository.findByProviderAndEmail(AuthProvider.LOCAL, email);

        // 없는 이메일이어도 해싱을 건너뛰지 않는다. 건너뛰면 응답이 눈에 띄게 빨라져
        // 메시지를 통일해도 소요 시간으로 가입 여부를 알아낼 수 있다.
        String hash = credential.map(UserCredential::getPasswordHash).orElse(dummyHash);
        boolean matched = passwordEncoder.matches(rawPassword, hash);

        if (credential.isEmpty() || !matched) {
            throw new IllegalArgumentException(FAILURE_MESSAGE);
        }
        return credential.get().getUser().getId();
    }
}
