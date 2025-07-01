package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.ApiResponseDTO;
import com.ecommerce.order_service.dto.OrderRequestDTO;
import com.ecommerce.order_service.dto.ShippingAddressDTO;
import com.ecommerce.order_service.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO> placeOrder(@RequestBody OrderRequestDTO request, JwtAuthenticationToken principal) {
        return ResponseEntity.ok(orderService.placeOrder(request, principal));
    }

//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponseDTO> getAllOrders(@RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponseDTO> getOrdersByUser(JwtAuthenticationToken principal,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(orderService.getOrdersByUserKeycloakId(principal, page, size));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponseDTO> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @GetMapping("/user/status")
    public ResponseEntity<ApiResponseDTO> getMyOrdersByStatus(@RequestParam String status,
                                                              JwtAuthenticationToken principal,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(orderService.getOrdersByUserAndStatus(principal, status, page, size));
    }

    @PutMapping("/{id}/shipping-address")
    public ResponseEntity<ApiResponseDTO> updateShippingAddress(@PathVariable Long id, @RequestBody ShippingAddressDTO shippingAddressDTO) {
        return ResponseEntity.ok(orderService.updateShippingAddress(id, shippingAddressDTO));
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<ApiResponseDTO> cancelOrder(@PathVariable Long id, JwtAuthenticationToken principal) {
        return ResponseEntity.ok(orderService.cancelOrder(id, principal));
    }


}
