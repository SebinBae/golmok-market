package com.golmok.core.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    boolean existsByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    Optional<User> findByPhoneNumberAndDeletedAtIsNull(String phoneNumber);
}
