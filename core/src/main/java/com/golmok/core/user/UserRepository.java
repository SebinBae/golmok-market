package com.golmok.core.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    boolean existsByPhoneNumberAndDeletedAtIsNull(String phoneNumber);
}
