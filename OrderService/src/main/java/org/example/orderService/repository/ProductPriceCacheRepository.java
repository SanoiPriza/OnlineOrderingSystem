package org.example.orderService.repository;

import org.example.orderService.model.ProductPriceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductPriceCacheRepository extends JpaRepository<ProductPriceCache, String> {
}
