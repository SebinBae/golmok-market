package com.golmok.core.product;

import com.golmok.core.region.Region;
import com.golmok.core.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product")
public class Product {

    private static final int TITLE_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int PRICE_MAX = 100_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /** 등록 시점의 동네로 고정된다. 판매자가 동네를 바꿔도 움직이지 않는다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deletedAt;

    protected Product() {
    }

    public static Product register(User seller, Region region, String title, String description,
                                   int price, Category category, Instant now) {
        validate(title, description, price);

        Product product = new Product();
        product.seller = seller;
        product.region = region;
        product.title = title;
        product.description = description;
        product.price = price;
        product.category = category;
        product.status = ProductStatus.ON_SALE;
        product.createdAt = now;
        product.updatedAt = now;
        return product;
    }

    private static void validate(String title, String description, int price) {
        if (title == null || title.isBlank() || title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("제목은 1~%d자여야 합니다.".formatted(TITLE_MAX_LENGTH));
        }
        if (description == null || description.isBlank() || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("설명은 1~%d자여야 합니다.".formatted(DESCRIPTION_MAX_LENGTH));
        }
        if (price < 0 || price > PRICE_MAX) {
            throw new IllegalArgumentException("가격은 0원 이상 %d원 이하여야 합니다.".formatted(PRICE_MAX));
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isOwnedBy(Long userId) {
        return seller.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public User getSeller() {
        return seller;
    }

    public Region getRegion() {
        return region;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
