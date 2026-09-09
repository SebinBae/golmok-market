package com.golmok.core.user;

import com.golmok.core.region.Region;
import com.golmok.core.region.RegionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserRegionService {

    private static final int MAX_REGIONS = 2;

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final UserRegionRepository userRegionRepository;

    public UserRegionService(UserRepository userRepository,
                             RegionRepository regionRepository,
                             UserRegionRepository userRegionRepository) {
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.userRegionRepository = userRegionRepository;
    }

    public Long register(Long userId, Long regionId) {
        User user = findUser(userId);
        Region region = findRegion(regionId);

        if (userRegionRepository.existsByUserAndRegion(user, region)) {
            throw new IllegalStateException("이미 등록한 동네입니다. regionId=" + regionId);
        }
        if (userRegionRepository.countByUser(user) >= MAX_REGIONS) {
            throw new IllegalStateException("동네는 최대 " + MAX_REGIONS + "개까지 등록할 수 있습니다.");
        }

        UserRegion saved = userRegionRepository.save(UserRegion.of(user, region, Instant.now()));
        return saved.getId();
    }

    public void unregister(Long userId, Long regionId) {
        User user = findUser(userId);
        Region region = findRegion(regionId);

        UserRegion userRegion = userRegionRepository.findByUserAndRegion(user, region)
                .orElseThrow(() -> new IllegalStateException("등록하지 않은 동네입니다. regionId=" + regionId));
        userRegionRepository.delete(userRegion);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));
    }

    private Region findRegion(Long regionId) {
        return regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 동네입니다. regionId=" + regionId));
    }
}
