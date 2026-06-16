package org.example.orderService.service;

import org.example.orderService.client.ProductServiceClient;
import org.example.orderService.dto.ProductResponse;

import org.example.common.exception.InvalidOperationException;
import org.example.common.exception.ResourceNotFoundException;
import org.example.common.model.OrderStatus;
import org.example.orderService.dto.PaymentResponse;
import org.example.orderService.dto.OrderRequest;
import org.example.orderService.dto.OrderResponse;
import org.example.orderService.mapper.OrderMapper;
import org.example.orderService.model.OrderEntity;
import org.example.orderService.repository.OrderRepository;
import org.example.orderService.repository.OutboxEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.example.orderService.model.ProcessedEvent;
import org.example.orderService.repository.ProcessedEventRepository;
import org.example.common.event.StockReservedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.example.orderService.event.OutboxSavedEvent;
import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;
    private final OrderPaymentProcessor orderPaymentProcessor;
    private final ProductServiceClient productServiceClient;
    private final ProcessedEventRepository processedEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private OrderService self;

    public OrderService(OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            OrderMapper orderMapper,
            OrderPaymentProcessor orderPaymentProcessor,
            ProductServiceClient productServiceClient,
            ProcessedEventRepository processedEventRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.orderMapper = orderMapper;
        this.orderPaymentProcessor = orderPaymentProcessor;
        this.productServiceClient = productServiceClient;
        this.processedEventRepository = processedEventRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public List<OrderResponse> getMyOrders() {
        String username = resolveCurrentUsername();
        return orderRepository.findByUsername(username).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public Optional<OrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id).map(orderMapper::toResponse);
    }

    public OrderResponse createOrder(OrderRequest orderRequest) {
        String productId = orderRequest.getProductId();
        BigDecimal productPrice;
        String currency = "USD";
        
        try {
            ProductResponse product = productServiceClient.getProductById(productId);
            if (product == null || product.getPrice() == null) {
                throw new ResourceNotFoundException("Product not found or has no price");
            }
            productPrice = product.getPrice();
        } catch (Exception e) {
            throw new InvalidOperationException("Failed to fetch product " + productId + " from ProductService: " + e.getMessage());
        }

        BigDecimal totalAmount = productPrice.multiply(BigDecimal.valueOf(orderRequest.getQuantity()));
        String username = resolveCurrentUsername();

        return self.saveOrderInTransaction(orderRequest, productId, totalAmount, currency, username);
    }

    @Transactional
    public OrderResponse saveOrderInTransaction(OrderRequest orderRequest, String productId, BigDecimal totalAmount, String currency, String username) {
        OrderEntity order = new OrderEntity();
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUsername(username);
        order.setProductId(productId);
        order.setQuantity(orderRequest.getQuantity());
        order.setAmount(totalAmount);
        order.setCurrency(currency);
        order.setPaymentMethod(orderRequest.getPaymentMethod());

        OrderEntity savedOrder = orderRepository.save(order);

        org.example.orderService.model.OutboxEvent event = org.example.orderService.model.OutboxEvent.orderCreated(
                savedOrder.getId(), orderRequest.getProductId(), orderRequest.getQuantity());
        outboxEventRepository.save(event);
        applicationEventPublisher.publishEvent(new OutboxSavedEvent(this));

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public void initiateOrderPayment(Long orderId) {
        org.example.orderService.model.OutboxEvent outboxEvent = org.example.orderService.model.OutboxEvent.initiatePayment(orderId);
        outboxEventRepository.save(outboxEvent);
        applicationEventPublisher.publishEvent(new org.example.orderService.event.OutboxSavedEvent(this));
    }

    @Transactional
    public void processStockReserved(StockReservedEvent event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            return; // Already processed
        }

        Long orderId = Long.parseLong(event.getOrderId());
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus().canTransitionTo(OrderStatus.STOCK_RESERVED)) {
            order.setStatus(OrderStatus.STOCK_RESERVED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        org.example.orderService.model.OutboxEvent outboxEvent = org.example.orderService.model.OutboxEvent.initiatePayment(orderId);
        outboxEventRepository.save(outboxEvent);
        applicationEventPublisher.publishEvent(new OutboxSavedEvent(this));

        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId()));
        }
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        if (!order.getStatus().canTransitionTo(status)) {
            throw new InvalidOperationException(
                    "Invalid order status transition: " + order.getStatus() + " -> " + status);
        }
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public CompletableFuture<OrderEntity> refundOrderPaymentAsync(Long id) {
        return orderPaymentProcessor.refundPaymentAsync(id);
    }

    @Transactional
    public OrderResponse refundOrderPayment(Long id) {
        try {
            return orderMapper.toResponse(orderPaymentProcessor.refundPaymentAsync(id).get());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error refunding payment: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", "id", id);
        }
        orderRepository.deleteById(id);
    }

    public List<OrderResponse> getOrdersByCustomerName(String customerName) {
        return orderRepository.findByCustomerName(customerName).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public List<OrderResponse> getOrdersByProductId(String productId) {
        return orderRepository.findByProductId(productId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public CompletableFuture<PaymentResponse> getOrderPaymentStatusAsync(Long id) {
        return orderPaymentProcessor.getPaymentStatusAsync(id);
    }

    public PaymentResponse getOrderPaymentStatus(Long id) {
        try {
            return orderPaymentProcessor.getPaymentStatusAsync(id).get();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error getting payment status: " + e.getMessage());
        }
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }
}
