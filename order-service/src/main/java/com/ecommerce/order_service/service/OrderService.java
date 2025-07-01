package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.exception.OrderServiceException;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.utils.OrderMapper;
import com.ecommerce.order_service.utils.ShippingAddressMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class OrderService {

    private final InterServiceCall interServiceCall;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ShippingAddressMapper shippingAddressMapper;
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);


    public OrderService(InterServiceCall interServiceCall, OrderRepository orderRepository, OrderMapper orderMapper, ShippingAddressMapper shippingAddressMapper) {
        this.interServiceCall = interServiceCall;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.shippingAddressMapper = shippingAddressMapper;
    }

    public ApiResponseDTO placeOrder(OrderRequestDTO request, JwtAuthenticationToken principal) {
        String token = principal.getToken().getTokenValue();
        String userKeycloakId = principal.getToken().getSubject();
        List<OrderItemDTO> items = request.getOrderItems();

        logger.info("Initiating order placement for user: {}", userKeycloakId);

        for (OrderItemDTO item : items) {
            logger.debug("Checking inventory for productId: {}", item.getProductId());
            InventoryDTO inventory = interServiceCall.getInventoryByProductId(item.getProductId(), token);

            if (inventory == null) {
                logger.error("Product not found: {}", item.getProductId());
                throw new OrderServiceException("Product ID " + item.getProductId() + " not found", HttpStatus.NOT_FOUND);
            }

            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                logger.warn("Insufficient stock for product: {} (requested: {}, available: {})",
                        item.getProductName(), item.getQuantity(), inventory.getAvailableQuantity());
                throw new OrderServiceException("Insufficient stock for product " + item.getProductName(), HttpStatus.BAD_REQUEST);
            }

        }

        for (OrderItemDTO item : items) {
            logger.debug("Reserving stock for productId: {}, quantity: {}", item.getProductId(), item.getQuantity());
            interServiceCall.reserveStock(item.getProductId(), item.getQuantity(), token);
        }

        Order order = orderMapper.toEntity(request);
        order.setUserKeycloakId(userKeycloakId);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        order = orderRepository.save(order);
        logger.info("Order saved with ID: {}", order.getId());
        try {
            logger.debug("Attempting to clear cart for user: {}", userKeycloakId);
            interServiceCall.clearCart(token);
            logger.info("Cart cleared for user: {}", userKeycloakId);
        } catch (Exception ex) {
            logger.warn("Cart cleanup failed for user {}: {}", userKeycloakId, ex.getMessage());
        }
        logger.info("Order placed successfully for user: {}", userKeycloakId);
        return new ApiResponseDTO("success", "Order placed successfully", order);
    }


    public ApiResponseDTO getAllOrders(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders;
        if (status != null && !status.isBlank()) {
            // Only fetch orders with the specified status
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            //  Fetch all orders if status is not provided
            orders = orderRepository.findAll(pageable);
        }
        return new ApiResponseDTO("success", "Orders fetched successfully", orders);
    }

    public ApiResponseDTO getOrderById(Long id) {
        return new ApiResponseDTO("success", "Order fetched successfully",
                orderRepository.findById(id).orElseThrow(() -> new OrderServiceException("Order not found", HttpStatus.NOT_FOUND)));
    }

    public ApiResponseDTO getOrdersByUserKeycloakId(JwtAuthenticationToken principal, int page, int size) {
        String userKeycloakId = principal.getToken().getSubject();
        Pageable pageable = PageRequest.of(page, size);
        List<Order> orders = orderRepository.findByUserKeycloakIdOrderByOrderDateDesc(userKeycloakId, pageable);

        if (orders.isEmpty()) {
            throw new OrderServiceException("No orders found for user ID: " + userKeycloakId, HttpStatus.NOT_FOUND);
        }

        return new ApiResponseDTO("success", "Orders fetched for user", orders);
    }

    public ApiResponseDTO updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderServiceException("Order not found", HttpStatus.NOT_FOUND));

        order.setStatus(status);
        order = orderRepository.save(order);

        return new ApiResponseDTO("success", "Order status updated", order);
    }


    public ApiResponseDTO getOrdersByUserAndStatus(JwtAuthenticationToken principal, String status, int page, int size) {
        String userKeycloakId = principal.getToken().getSubject();
        Pageable pageable = PageRequest.of(page, size);
        List<Order> orders = orderRepository.findByUserKeycloakIdAndStatusOrderByOrderDateDesc(userKeycloakId, status, pageable);

        if (orders.isEmpty()) {
            throw new OrderServiceException("No orders found with status " + status, HttpStatus.NOT_FOUND);
        }

        return new ApiResponseDTO("success", "Orders fetched for user with status: " + status, orders);
    }

    public ApiResponseDTO updateShippingAddress(Long id, ShippingAddressDTO shippingAddressDTO) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderServiceException("Order not found", HttpStatus.NOT_FOUND));

        // Assuming shipping address is embedded or part of Order entity
        order.setShippingAddress(shippingAddressMapper.toEntity(shippingAddressDTO));

        orderRepository.save(order);

        return new ApiResponseDTO("success", "Shipping address updated successfully", order);
    }

    public ApiResponseDTO cancelOrder(Long id, JwtAuthenticationToken principal) {
        String token = principal.getToken().getTokenValue();
        String userKeycloakId = principal.getToken().getSubject();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderServiceException("Order not found", HttpStatus.NOT_FOUND));

        // Optional: Check if user owns the order
        if (!order.getUserKeycloakId().equals(userKeycloakId)) {
            throw new OrderServiceException("You are not authorized to cancel this order", HttpStatus.UNAUTHORIZED);
        }

        // Optional: Prevent cancellation if already cancelled or delivered
        if ("CANCELLED".equalsIgnoreCase(order.getStatus()) || "DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new OrderServiceException("Order cannot be cancelled", HttpStatus.BAD_REQUEST);
        }

        // Step 1: Rollback reserved inventory
        for (OrderItem item : order.getOrderItems()) {
            interServiceCall.releaseStock(item.getProductId(), item.getQuantity(), token);
        }

        // Step 2: Update order status
        order.setStatus("CANCELLED");
        orderRepository.save(order);

        return new ApiResponseDTO("success", "Order cancelled and stock released", order);
    }
}
