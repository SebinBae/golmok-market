package com.golmok.core.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FindEmailService {

    private final VerificationCodeService verificationCodeService;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;

    public FindEmailService(VerificationCodeService verificationCodeService,
                            UserRepository userRepository,
                            UserCredentialRepository userCredentialRepository) {
        this.verificationCodeService = verificationCodeService;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
    }

    /** 마스킹된 이메일을 돌려준다. 평문은 core 밖으로 나가지 않는다. */
    public String findEmail(String phoneNumber, String rawCode) {
        verificationCodeService.verify(phoneNumber, VerificationPurpose.FIND_EMAIL, rawCode);

        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(phoneNumber)
                .orElseThrow(() -> new IllegalStateException("해당 번호로 가입한 내역이 없습니다."));

        UserCredential credential = userCredentialRepository
                .findByUserAndProvider(user, AuthProvider.LOCAL)
                .orElseThrow(() -> new IllegalStateException("해당 번호로 가입한 내역이 없습니다."));

        return EmailMasker.mask(credential.getEmail());
    }
}
