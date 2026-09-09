package com.golmok.core.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

    boolean existsByProviderAndEmail(AuthProvider provider, String email);

    Optional<UserCredential> findByProviderAndEmail(AuthProvider provider, String email);
}
