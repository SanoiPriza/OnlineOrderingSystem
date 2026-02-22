package org.example.orderService.controller;

import jakarta.validation.Valid;
import org.example.common.model.OrderStatus;
import org.example.orderService.dto.OrderRequest;
import org.example.orderService.dto.OrderResponse;
import org.example.orderService.dto.PaymentResponse;
import org.example.orderService.mapper.OrderMapper;
import org.example.orderService.model.OrderEntity;
import org.example.orderService.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private static final long ASYNC_TIMEOUT = 10000L;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        Optional<OrderResponse> order = orderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest order) {
        return orderService.createOrder(order);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        OrderResponse updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{id}/payment")
    public DeferredResult<ResponseEntity<OrderResponse>> processOrderPayment(@PathVariable Long id) {
        DeferredResult<ResponseEntity<OrderResponse>> deferredResult = new DeferredResult<>(ASYNC_TIMEOUT);

        if (orderService.getOrderById(id).isPresent()) {
            CompletableFuture<OrderEntity> future = orderService.processOrderPaymentAsync(id);
            future.thenAccept(processedOrder -> {
                if (processedOrder != null) {
                    deferredResult.setResult(ResponseEntity.ok(orderMapper.toResponse(processedOrder)));
                } else {
                    deferredResult.setResult(ResponseEntity.notFound().build());
                }
            }).exceptionally(ex -> {
                deferredResult.setErrorResult(ResponseEntity.badRequest().build());
                return null;
            });
        } else {
            deferredResult.setResult(ResponseEntity.notFound().build());
        }

        return deferredResult;
    }

    @GetMapping("/{id}/payment")
    public DeferredResult<ResponseEntity<PaymentResponse>> getOrderPaymentStatus(@PathVariable Long id) {
        DeferredResult<ResponseEntity<PaymentResponse>> deferredResult = new DeferredResult<>(ASYNC_TIMEOUT);

        CompletableFuture<PaymentResponse> future = orderService.getOrderPaymentStatusAsync(id);

        future.thenAccept(paymentStatus -> deferredResult.setResult(ResponseEntity.ok(paymentStatus)))
                .exceptionally(ex -> {
                    deferredResult.setErrorResult(ResponseEntity.notFound().build());
                    return null;
                });

        return deferredResult;
    }

    @PostMapping("/{id}/refund")
    public DeferredResult<ResponseEntity<OrderResponse>> refundOrderPayment(@PathVariable Long id) {
        DeferredResult<ResponseEntity<OrderResponse>> deferredResult = new DeferredResult<>(ASYNC_TIMEOUT);

        CompletableFuture<OrderEntity> future = orderService.refundOrderPaymentAsync(id);

        future.thenAccept(refundedOrder -> {
            if (refundedOrder != null) {
                deferredResult.setResult(ResponseEntity.ok(orderMapper.toResponse(refundedOrder)));
            } else {
                deferredResult.setResult(ResponseEntity.notFound().build());
            }
        }).exceptionally(ex -> {
            deferredResult.setErrorResult(ResponseEntity.notFound().build());
            return null;
        });

        return deferredResult;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customer/{customerName}")
    public List<OrderResponse> getOrdersByCustomerName(@PathVariable String customerName) {
        return orderService.getOrdersByCustomerName(customerName);
    }

    @GetMapping("/status/{status}")
    public List<OrderResponse> getOrdersByStatus(@PathVariable OrderStatus status) {
        return orderService.getOrdersByStatus(status);
    }

    @GetMapping("/health")
    public String getStatus() {
        return "Order Service is running!";
    }
}
