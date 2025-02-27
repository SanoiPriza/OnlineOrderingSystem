package org.example.productService.service;

import org.example.productService.model.Product;
import org.example.productService.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(String id, Product productDetails) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isPresent()) {
            Product existingProduct = optionalProduct.get();
            existingProduct.setName(productDetails.getName());
            existingProduct.setPrice(productDetails.getPrice());
            return productRepository.save(existingProduct);
        }
        throw new RuntimeException("Product not found with id: " + id);
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    public List<Product> findByName(String name) {
        return productRepository.findByName(name);
    }

    public List<Product> findByPriceLessThan(double price) {
        return productRepository.findByPriceLessThan(price);
    }

    public List<Product> findByPriceGreaterThan(double price) {
        return productRepository.findByPriceGreaterThan(price);
    }
}