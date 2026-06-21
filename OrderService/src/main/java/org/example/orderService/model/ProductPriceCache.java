package org.example.orderService.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_price_cache")
public class ProductPriceCache {

    @Id
    private String productId;

    private String name;
    private BigDecimal price;
    private LocalDateTime updatedAt;

    public ProductPriceCache() {
    }

    public ProductPriceCache(String productId, String name, BigDecimal price) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
