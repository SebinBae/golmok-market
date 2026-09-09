package com.golmok.core.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 값 검증은 Spring 없이 확인한다. */
class ProductTest {

    private Product register(String title, String description, int price) {
        return Product.register(null, null, title, description, price, Category.DIGITAL, Instant.now());
    }

    @Test
    void 등록하면_판매중_상태로_시작한다() {
        Product product = register("제목", "설명", 10_000);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.isDeleted()).isFalse();
    }

    @Test
    void 가격_0원은_나눔이라_허용한다() {
        assertThat(register("제목", "설명", 0).getPrice()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void 제목이_비면_거부한다(String title) {
        assertThatThrownBy(() -> register(title, "설명", 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("제목");
    }

    @Test
    void 제목이_백_자를_넘으면_거부한다() {
        assertThatThrownBy(() -> register("가".repeat(101), "설명", 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("제목");
    }

    @Test
    void 제목_백_자는_허용한다() {
        assertThat(register("가".repeat(100), "설명", 1000).getTitle()).hasSize(100);
    }

    @Test
    void 설명이_이천_자를_넘으면_거부한다() {
        assertThatThrownBy(() -> register("제목", "가".repeat(2001), 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("설명");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 100_000_001})
    void 가격이_범위를_벗어나면_거부한다(int price) {
        assertThatThrownBy(() -> register("제목", "설명", price))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("가격");
    }

    @Test
    void 가격_일억은_허용한다() {
        assertThat(register("제목", "설명", 100_000_000).getPrice()).isEqualTo(100_000_000);
    }
}
