package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.dto.ApiResponseDTO;
import com.ecommerce.cart_service.dto.CartItemDTO;
import com.ecommerce.cart_service.exception.CartServiceException;
import com.ecommerce.cart_service.model.Cart;
import com.ecommerce.cart_service.model.CartItem;
import com.ecommerce.cart_service.repository.CartItemRepository;
import com.ecommerce.cart_service.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public ApiResponseDTO addItemToCart(CartItemDTO itemDTO, JwtAuthenticationToken principal) {
        String userKeycloakId = principal.getToken().getSubject();
        logger.info("Adding item to cart for userKeycloakId: {}", userKeycloakId);
        try {
            Cart cart = getCartByUserId(userKeycloakId);

            CartItem item = new CartItem();
            item.setProductId(itemDTO.getProductId());
            item.setProductName(itemDTO.getProductName());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(itemDTO.getPrice());

            cart.getItems().add(item); // Add to cart
            Cart savedCart = cartRepository.save(cart); // Save cart with cascade

            logger.info("Item successfully added to cart for user: {}", userKeycloakId);
            return new ApiResponseDTO("success", "Item added to cart successfully", savedCart);

        } catch (Exception e) {
            logger.error("Error while adding item to cart for user: {}", userKeycloakId, e);
            throw new CartServiceException("Unable to add item to cart", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Cart getCartByUserId(String userKeycloakId) {
        return cartRepository.findByUserKeycloakId(userKeycloakId)
                .orElseGet(() -> {
                    logger.info("Cart not found for userKeycloakId: {}. Creating new cart.", userKeycloakId);
                    Cart newCart = new Cart();
                    newCart.setUserKeycloakId(userKeycloakId);
                    return cartRepository.save(newCart);
                });
    }

    public ApiResponseDTO getItemsFromCart(JwtAuthenticationToken principal) {
        String userKeycloakId = principal.getToken().getSubject();
        logger.info("Fetching cart items for userKeycloakId: {}", userKeycloakId);
        try {
            Cart cart = cartRepository.findByUserKeycloakId(userKeycloakId)
                    .orElseThrow(() -> {
                        logger.warn("Cart not found for user: {}", userKeycloakId);
                        return new CartServiceException(
                                "Cart not found for user: " + userKeycloakId,
                                HttpStatus.NOT_FOUND
                        );
                    });

            logger.info("Successfully fetched cart items for user: {}", userKeycloakId);
            return new ApiResponseDTO("success", "Items fetched from cart successfully", cart);

        } catch (CartServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error while fetching cart for user: {}", userKeycloakId, e);
            throw new CartServiceException("Unable to fetch cart", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponseDTO updateItemQuantity(JwtAuthenticationToken principal, Long itemId, int amount) {
        String userKeycloakId = principal.getToken().getSubject();
        logger.info("Updating item quantity for itemId: {}, delta: {}, user: {}", itemId, amount, userKeycloakId);

        Cart cart = cartRepository.findByUserKeycloakId(userKeycloakId)
                .orElseThrow(() -> new CartServiceException("Cart not found for user: " + userKeycloakId, HttpStatus.NOT_FOUND));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartServiceException("Item not found in user's cart", HttpStatus.NOT_FOUND));

        int updatedQuantity = item.getQuantity() + amount;

        if (updatedQuantity <= 0) {
            cart.getItems().remove(item);
            logger.info("Quantity is zero or less. Removing item from cart.");
        } else {
            item.setQuantity(updatedQuantity);
            logger.info("Quantity updated to {}", updatedQuantity);
        }

        Cart updatedCart = cartRepository.save(cart);

        return new ApiResponseDTO("success", "Cart item quantity updated successfully", updatedCart);
    }
}
