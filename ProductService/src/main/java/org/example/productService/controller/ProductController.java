package org.example.productService.controller;

import jakarta.validation.Valid;
import org.example.productService.dto.ProductRequest;
import org.example.productService.dto.ProductResponse;
import org.example.productService.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest product) {
        return productService.createProduct(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest productDetails) {
        ProductResponse updatedProduct = productService.updateProduct(id, productDetails);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/name/{name}")
    public List<ProductResponse> findByName(@PathVariable String name) {
        return productService.findByName(name);
    }

    @GetMapping("/search/price-less-than/{price}")
    public List<ProductResponse> findByPriceLessThan(@PathVariable BigDecimal price) {
        return productService.findByPriceLessThan(price);
    }

    @GetMapping("/search/price-greater-than/{price}")
    public List<ProductResponse> findByPriceGreaterThan(@PathVariable BigDecimal price) {
        return productService.findByPriceGreaterThan(price);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<Integer> getStock(@PathVariable String id) {
        return ResponseEntity.ok(productService.getStockQuantity(id));
    }

    @PutMapping("/{id}/stock/decrement")
    public ResponseEntity<ProductResponse> decrementStock(
            @PathVariable String id,
            @RequestParam int quantity) {
        return ResponseEntity.ok(productService.decrementStock(id, quantity));
    }

    @PutMapping("/{id}/stock/increment")
    public ResponseEntity<ProductResponse> incrementStock(
            @PathVariable String id,
            @RequestParam int quantity) {
        return ResponseEntity.ok(productService.incrementStock(id, quantity));
    }

    @GetMapping("/health")
    public String getStatus() {
        return "Product Service is running!";
    }
}