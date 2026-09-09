package com.golmok.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.golmok.core.region.Region;
import com.golmok.core.region.RegionRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserRegionServiceTest {

    @Autowired
    private UserRegionService userRegionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private UserRegionRepository userRegionRepository;

    private User user;
    private List<Region> regions;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.create("테스터", "01000000001", Instant.now()));
        regions = regionRepository.findAll();
    }

    @Test
    void 동네를_두_개까지_등록한다() {
        userRegionService.register(user.getId(), regions.get(0).getId());
        userRegionService.register(user.getId(), regions.get(1).getId());

        assertThat(userRegionRepository.countByUser(user)).isEqualTo(2);
    }

    @Test
    void 세_번째_동네는_거부한다() {
        userRegionService.register(user.getId(), regions.get(0).getId());
        userRegionService.register(user.getId(), regions.get(1).getId());

        assertThatThrownBy(() -> userRegionService.register(user.getId(), regions.get(2).getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최대 2개");

        assertThat(userRegionRepository.countByUser(user)).isEqualTo(2);
    }

    @Test
    void 같은_동네를_두_번_등록할_수_없다() {
        userRegionService.register(user.getId(), regions.get(0).getId());

        assertThatThrownBy(() -> userRegionService.register(user.getId(), regions.get(0).getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 등록");
    }

    @Test
    void 없는_동네는_등록할_수_없다() {
        assertThatThrownBy(() -> userRegionService.register(user.getId(), 99_999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 삭제한_동네를_다시_등록할_수_있다() {
        userRegionService.register(user.getId(), regions.get(0).getId());
        userRegionService.unregister(user.getId(), regions.get(0).getId());

        userRegionService.register(user.getId(), regions.get(0).getId());

        assertThat(userRegionRepository.countByUser(user)).isEqualTo(1);
    }
}
