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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/my")
    public List<OrderResponse> getMyOrders() {
        return orderService.getMyOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        Optional<OrderResponse> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        OrderResponse order = orderOpt.get();
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && (auth == null || !java.util.Objects.equals(order.getUsername(), auth.getName()))) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest order) {
        return orderService.createOrder(order);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        OrderResponse updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<Void> processOrderPayment(@PathVariable Long id) {
        Optional<OrderResponse> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isPresent()) {
            OrderResponse order = orderOpt.get();
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (!isAdmin && (auth == null || !java.util.Objects.equals(order.getUsername(), auth.getName()))) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }

            orderService.initiateOrderPayment(id);
            return ResponseEntity.accepted().build();
        } else {
            return ResponseEntity.notFound().build();
        }
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customer/{customerName}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> getOrdersByCustomerName(@PathVariable String customerName) {
        return orderService.getOrdersByCustomerName(customerName);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> getOrdersByStatus(@PathVariable OrderStatus status) {
        return orderService.getOrdersByStatus(status);
    }

    @GetMapping("/health")
    public String getStatus() {
        return "Order Service is running!";
    }
}
