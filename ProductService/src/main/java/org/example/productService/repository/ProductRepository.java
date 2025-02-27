package org.example.productService.repository;

import org.example.productService.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByName(String name);
    List<Product> findByPriceLessThan(double price);
    List<Product> findByPriceGreaterThan(double price);
}