package com.golmok.core.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.golmok.core.region.Region;
import com.golmok.core.region.RegionRepository;
import com.golmok.core.user.User;
import com.golmok.core.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductPersistenceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private User seller;
    private Region region;

    @BeforeEach
    void setUp() {
        seller = userRepository.save(User.create("판매자", "01011112222", Instant.now()));
        region = regionRepository.findAll().get(0);
    }

    private Product saveProduct(Category category) {
        return productRepository.save(Product.register(
                seller, region, "아이패드 프로", "거의 새것입니다", 500_000, category, Instant.now()));
    }

    @Test
    void enum은_숫자가_아니라_문자열로_저장된다() {
        Product product = saveProduct(Category.DIGITAL);
        entityManager.flush();

        String category = jdbcTemplate.queryForObject(
                "SELECT category FROM product WHERE id = ?", String.class, product.getId());
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM product WHERE id = ?", String.class, product.getId());

        assertThat(category).isEqualTo("DIGITAL");
        assertThat(status).isEqualTo("ON_SALE");
    }

    @Test
    void 등록한_동네와_판매자가_그대로_붙는다() {
        Product saved = saveProduct(Category.DIGITAL);
        entityManager.flush();
        entityManager.clear();

        Product found = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getSeller().getId()).isEqualTo(seller.getId());
        assertThat(found.getRegion().getId()).isEqualTo(region.getId());
        assertThat(found.isOwnedBy(seller.getId())).isTrue();
        assertThat(found.isOwnedBy(seller.getId() + 1)).isFalse();
    }

    /**
     * id 전략이 IDENTITY라 save() 시점에 INSERT가 나간다 (ADR 0007).
     * 제약 위반이 flush가 아니라 save에서 터진다.
     */
    @Test
    void 같은_상품에_같은_순서의_이미지를_둘_수_없다() {
        Product product = saveProduct(Category.DIGITAL);
        productImageRepository.save(ProductImage.of(product, "https://picsum.photos/seed/1/400/300", 0));
        productImageRepository.save(ProductImage.of(product, "/images/a.jpg", 1));

        assertThatThrownBy(() -> productImageRepository.save(ProductImage.of(product, "/images/b.jpg", 0)))
                .hasStackTraceContaining("uk_product_image_sort_order");
    }
}
