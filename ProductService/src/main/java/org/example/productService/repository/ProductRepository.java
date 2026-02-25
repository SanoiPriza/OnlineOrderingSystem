package org.example.productService.repository;

import org.example.productService.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByName(String name);

    List<Product> findByPriceLessThan(BigDecimal price);

    List<Product> findByPriceGreaterThan(BigDecimal price);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity "
            + "WHERE p.id = :id AND p.stockQuantity >= :quantity")
    int atomicDecrementStock(@Param("id") String id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :quantity WHERE p.id = :id")
    int atomicIncrementStock(@Param("id") String id, @Param("quantity") int quantity);
}