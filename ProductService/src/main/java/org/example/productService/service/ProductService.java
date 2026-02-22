package org.example.productService.service;

import org.example.common.exception.InvalidOperationException;
import org.example.common.exception.ResourceNotFoundException;
import org.example.productService.dto.ProductRequest;
import org.example.productService.dto.ProductResponse;
import org.example.productService.mapper.ProductMapper;
import org.example.productService.model.Product;
import org.example.productService.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<ProductResponse> getProductById(String id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse);
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        productMapper.updateEntityFromRequest(request, existingProduct);
        Product updatedProduct = productRepository.save(existingProduct);
        return productMapper.toResponse(updatedProduct);
    }

    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        productRepository.deleteById(id);
    }

    public List<ProductResponse> findByName(String name) {
        return productRepository.findByName(name).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> findByPriceLessThan(BigDecimal price) {
        return productRepository.findByPriceLessThan(price).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> findByPriceGreaterThan(BigDecimal price) {
        return productRepository.findByPriceGreaterThan(price).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse decrementStock(String id, int quantity) {
        if (quantity <= 0) {
            throw new InvalidOperationException("Decrement quantity must be greater than zero");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        int available = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (available < quantity) {
            throw new InvalidOperationException(
                    "Insufficient stock for product '" + product.getName() + "'. "
                            + "Requested: " + quantity + ", available: " + available);
        }

        product.setStockQuantity(available - quantity);
        return productMapper.toResponse(productRepository.save(product));
    }

    public int getStockQuantity(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return product.getStockQuantity() != null ? product.getStockQuantity() : 0;
    }
}