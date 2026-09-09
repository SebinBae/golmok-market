package com.golmok.core.region;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RegionRepositoryTest {

    @Autowired
    private RegionRepository regionRepository;

    @Test
    void 마이그레이션이_적재한_강북구_행정동을_읽는다() {
        assertThat(regionRepository.findAll())
                .hasSize(13)
                .allSatisfy(region -> {
                    assertThat(region.getSido()).isEqualTo("서울특별시");
                    assertThat(region.getSigungu()).isEqualTo("강북구");
                });
    }
}
