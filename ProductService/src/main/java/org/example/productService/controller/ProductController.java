package org.example.productService.controller;

import jakarta.validation.Valid;
import org.example.productService.model.Product;
import org.example.productService.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product createProduct(@Valid @RequestBody Product product) {
        return productService.createProduct(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody Product productDetails) {
        Product updatedProduct = productService.updateProduct(id, productDetails);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/name/{name}")
    public List<Product> findByName(@PathVariable String name) {
        return productService.findByName(name);
    }

    @GetMapping("/search/price-less-than/{price}")
    public List<Product> findByPriceLessThan(@PathVariable BigDecimal price) {
        return productService.findByPriceLessThan(price);
    }

    @GetMapping("/search/price-greater-than/{price}")
    public List<Product> findByPriceGreaterThan(@PathVariable BigDecimal price) {
        return productService.findByPriceGreaterThan(price);
    }

    @GetMapping("/health")
    public String getStatus() {
        return "Product Service is running!";
    }
}