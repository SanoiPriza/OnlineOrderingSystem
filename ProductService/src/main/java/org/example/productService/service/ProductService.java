package org.example.productService.service;

import org.example.common.event.OrderCreatedEvent;
import org.example.common.event.ProductUpdatedEvent;
import org.example.common.event.StockCompensationEvent;
import org.example.common.exception.InvalidOperationException;
import org.example.common.exception.ResourceNotFoundException;
import org.example.productService.config.RabbitMQConfig;
import org.example.productService.dto.ProductRequest;
import org.example.productService.dto.ProductResponse;
import org.example.productService.mapper.ProductMapper;
import org.example.productService.model.ProcessedEvent;
import org.example.productService.model.Product;
import org.example.productService.repository.ProcessedEventRepository;
import org.example.productService.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final org.example.productService.repository.OutboxEventRepository outboxEventRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public ProductService(ProductRepository productRepository, ProductMapper productMapper, ProcessedEventRepository processedEventRepository, org.example.productService.repository.OutboxEventRepository outboxEventRepository) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.processedEventRepository = processedEventRepository;
        this.outboxEventRepository = outboxEventRepository;
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

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        publishProductUpdatedEvent(savedProduct);
        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        productMapper.updateEntityFromRequest(request, existingProduct);
        Product updatedProduct = productRepository.save(existingProduct);
        publishProductUpdatedEvent(updatedProduct);
        return productMapper.toResponse(updatedProduct);
    }

    @Transactional
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

    @Transactional
    public ProductResponse decrementStock(String id, int quantity) {
        if (quantity <= 0) {
            throw new InvalidOperationException("Decrement quantity must be greater than zero");
        }
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        int updatedRows = productRepository.atomicDecrementStock(id, quantity);
        if (updatedRows == 0) {
            throw new InvalidOperationException(
                    "Insufficient stock for product '" + existing.getName() + "'. "
                            + "Requested: " + quantity
                            + ", available: "
                            + (existing.getStockQuantity() != null ? existing.getStockQuantity() : 0));
        }
        return productMapper.toResponse(productRepository.findById(id).orElseThrow());
    }

    @Transactional
    public ProductResponse incrementStock(String id, int quantity) {
        if (quantity <= 0) {
            throw new InvalidOperationException("Increment quantity must be greater than zero");
        }
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        productRepository.atomicIncrementStock(id, quantity);
        return productMapper.toResponse(productRepository.findById(id).orElseThrow());
    }

    public int getStockQuantity(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return product.getStockQuantity() != null ? product.getStockQuantity() : 0;
    }

    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            return;
        }

        decrementStock(event.getProductId(), event.getQuantity());

        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId()));
        }

        try {
            org.example.common.event.StockReservedEvent reservedEvent = new org.example.common.event.StockReservedEvent(java.util.UUID.randomUUID().toString(), event.getOrderId());
            String payload = objectMapper.writeValueAsString(reservedEvent);
            org.example.productService.model.OutboxEvent outboxEvent = new org.example.productService.model.OutboxEvent(
                    RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.STOCK_RESERVED_ROUTING_KEY, payload);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize StockReservedEvent", e);
        }
    }

    @Transactional
    public void publishStockReservationFailed(String orderId, String errorMessage) {
        try {
            org.example.common.event.StockReservationFailedEvent failedEvent = new org.example.common.event.StockReservationFailedEvent(
                    java.util.UUID.randomUUID().toString(), orderId, errorMessage);
            String payload = objectMapper.writeValueAsString(failedEvent);
            org.example.productService.model.OutboxEvent outboxEvent = new org.example.productService.model.OutboxEvent(
                    RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, payload);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize StockReservationFailedEvent", e);
        }
    }

    @Transactional
    public void processStockCompensation(StockCompensationEvent event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            return;
        }

        incrementStock(event.getProductId(), event.getQuantity());

        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId()));
        }
    }

    private void publishProductUpdatedEvent(Product product) {
        ProductUpdatedEvent event = new ProductUpdatedEvent(product.getId(), product.getName(), product.getPrice());
        try {
            String payload = objectMapper.writeValueAsString(event);
            org.example.productService.model.OutboxEvent outboxEvent = new org.example.productService.model.OutboxEvent(
                    RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PRODUCT_UPDATED_ROUTING_KEY, payload);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ProductUpdatedEvent", e);
        }
    }
}