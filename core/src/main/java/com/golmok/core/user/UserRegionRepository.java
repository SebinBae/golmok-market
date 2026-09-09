package com.golmok.core.user;

import com.golmok.core.region.Region;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {

    long countByUser(User user);

    boolean existsByUserAndRegion(User user, Region region);

    Optional<UserRegion> findByUserAndRegion(User user, Region region);
}
