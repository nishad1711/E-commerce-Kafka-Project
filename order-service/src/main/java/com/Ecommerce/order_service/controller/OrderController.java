package com.Ecommerce.order_service.controller;

import com.Ecommerce.order_service.dto.CreateOrderRequest;
import com.Ecommerce.order_service.entity.Order;
import com.Ecommerce.order_service.entity.OrderStatus;
import com.Ecommerce.order_service.security.UserPrincipal;
import com.Ecommerce.order_service.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CreateOrderRequest secureRequest = new CreateOrderRequest(
                principal.userId(),
                request.items()
        );

        return ResponseEntity.ok(
                orderService.createOrder(secureRequest)
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (!isAdmin(principal) && !principal.userId().equals(userId)) {
            throw new AccessDeniedException(
                    "You cannot view another user's orders"
            );
        }

        return ResponseEntity.ok(
                orderService.getOrdersByUser(userId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Order order = orderService.getOrderById(id);

        if (!isAdmin(principal)
                && !principal.userId().equals(order.getUserId())) {
            throw new AccessDeniedException(
                    "You cannot view another user's order"
            );
        }

        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status
    ) {
        return ResponseEntity.ok(
                orderService.updateStatus(id, status)
        );
    }

    private boolean isAdmin(UserPrincipal principal) {
        return "ADMIN".equals(principal.role());
    }
}
