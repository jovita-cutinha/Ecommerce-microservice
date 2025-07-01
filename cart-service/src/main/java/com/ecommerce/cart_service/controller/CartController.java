package com.ecommerce.cart_service.controller;

import com.ecommerce.cart_service.dto.ApiResponseDTO;
import com.ecommerce.cart_service.dto.CartItemDTO;
import com.ecommerce.cart_service.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/item")
    public ResponseEntity<ApiResponseDTO> addItem(@RequestBody CartItemDTO itemDTO, JwtAuthenticationToken principal) {
        return ResponseEntity.ok(cartService.addItemToCart(itemDTO, principal));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/items")
    public ResponseEntity<ApiResponseDTO> getItems(JwtAuthenticationToken principal) {
        return ResponseEntity.ok(cartService.getItemsFromCart(principal));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/items/{itemId}/increase")
    public ResponseEntity<ApiResponseDTO> increaseItemQuantity(@PathVariable String itemId,
                                                               @RequestParam(defaultValue = "1") int amount,
                                                               JwtAuthenticationToken principal) {
        return ResponseEntity.ok(cartService.updateItemQuantity(principal, itemId, amount));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/items/{itemId}/decrease")
    public ResponseEntity<ApiResponseDTO> decreaseItemQuantity(@PathVariable String itemId,
                                                               @RequestParam(defaultValue = "1") int amount,
                                                               JwtAuthenticationToken principal) {
        return ResponseEntity.ok(cartService.updateItemQuantity(principal, itemId, -amount));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponseDTO> removeItemFromCart(@PathVariable String itemId,
                                                             JwtAuthenticationToken principal) {

        return ResponseEntity.ok(cartService.removeItemFromCart(principal, itemId));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping
    public ResponseEntity<ApiResponseDTO> deleteCart(JwtAuthenticationToken principal) {
        return ResponseEntity.ok(cartService.deleteCart(principal));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponseDTO> getCartSummary(JwtAuthenticationToken principal,
                                                         @RequestParam(defaultValue = "0") double discountPercent) {

        return ResponseEntity.ok(cartService.getCartSummary(principal, discountPercent));
    }


}
