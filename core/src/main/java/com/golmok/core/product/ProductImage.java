package com.golmok.core.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_image")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** mock의 외부 URL과 업로드된 로컬 경로가 섞인다. */
    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private int sortOrder;

    protected ProductImage() {
    }

    public static ProductImage of(Product product, String url, int sortOrder) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 비어 있습니다.");
        }
        if (sortOrder < 0) {
            throw new IllegalArgumentException("이미지 순서는 0 이상이어야 합니다.");
        }

        ProductImage image = new ProductImage();
        image.product = product;
        image.url = url;
        image.sortOrder = sortOrder;
        return image;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getUrl() {
        return url;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
