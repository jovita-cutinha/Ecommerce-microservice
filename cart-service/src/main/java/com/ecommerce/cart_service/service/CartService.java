package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.dto.ApiResponseDTO;
import com.ecommerce.cart_service.dto.CartItemDTO;
import com.ecommerce.cart_service.dto.CartSummaryDTO;
import com.ecommerce.cart_service.dto.InventoryResponseDTO;
import com.ecommerce.cart_service.exception.CartServiceException;
import com.ecommerce.cart_service.model.Cart;
import com.ecommerce.cart_service.model.CartItem;
import com.ecommerce.cart_service.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final InterServiceCall interServiceCall;
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    public CartService(CartRepository cartRepository, InterServiceCall interServiceCall) {
        this.cartRepository = cartRepository;
        this.interServiceCall = interServiceCall;
    }

    public ApiResponseDTO addItemToCart(CartItemDTO itemDTO, JwtAuthenticationToken principal) {
        String token = principal.getToken().getTokenValue();
        String userKeycloakId = principal.getToken().getSubject();
        logger.info("Adding item to cart for userKeycloakId: {}", userKeycloakId);
        try {
            // 1. Call Inventory Service to check stock
            String productId = itemDTO.getProductId();
            int requestedQty = itemDTO.getQuantity();

            // 3. Get cart and add item
            Cart cart = getCartByUserId(userKeycloakId);

            // 3. Check if item already exists in cart
            Optional<CartItem> existingItemOpt = cart.getItems().stream()
                    .filter(i -> i.getProductId().equals(productId))
                    .findFirst();

            int existingQty = existingItemOpt.map(item -> item.getQuantity()).orElse(0);
            int totalQty = existingQty + requestedQty;

            // 3. Inventory check ONCE for total quantity
            InventoryResponseDTO inventory = interServiceCall.getInventoryByProductId(productId, token);
            logger.info("Inventory check for productId {}: availableQuantity = {}", productId, inventory.getAvailableQuantity());

            if (inventory.getAvailableQuantity() < totalQty) {
                logger.warn("Insufficient stock for productId {}. Requested total: {}, Available: {}",
                        productId, totalQty, inventory.getAvailableQuantity());
                throw new CartServiceException("Insufficient stock available", HttpStatus.BAD_REQUEST);
            }

            // 4. Update quantity or add new item
            if (existingItemOpt.isPresent()) {
                CartItem existingItem = existingItemOpt.get();
                existingItem.setQuantity(totalQty);
                logger.info("Item already in cart. Quantity updated to {}", totalQty);
            } else {
                // 4. Add new item if it doesn’t exist
                CartItem newItem = new CartItem();
                newItem.setProductId(itemDTO.getProductId());
                newItem.setProductName(itemDTO.getProductName());
                newItem.setQuantity(itemDTO.getQuantity());
                newItem.setPrice(itemDTO.getPrice());

                cart.getItems().add(newItem);
                logger.info("Item not in cart. Adding new item.");
            }

            Cart savedCart = cartRepository.save(cart);
            logger.info("Cart saved successfully for user: {}", userKeycloakId);

            return new ApiResponseDTO("success", "Item added/updated in cart successfully", savedCart);

        } catch (CartServiceException e) {
            //  Let known service exceptions go through untouched
            throw e;
        } catch (Exception e) {
            logger.error("Error while adding item to cart for user: {}", userKeycloakId, e);
            throw new CartServiceException("Unable to add item to cart", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Fetches the cart for the given Keycloak user ID. If it doesn't exist, a new cart is created and saved.
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

    public ApiResponseDTO updateItemQuantity(JwtAuthenticationToken principal, String itemId, int amount) {
        String token = principal.getToken().getTokenValue();
        String userKeycloakId = principal.getToken().getSubject();
        logger.info("Updating item quantity for itemId: {}, delta: {}, user: {}", itemId, amount, userKeycloakId);

        Cart cart = cartRepository.findByUserKeycloakId(userKeycloakId)
                .orElseThrow(() -> new CartServiceException("Cart not found for user: " + userKeycloakId, HttpStatus.NOT_FOUND));

        // Locate the specific item in the cart by product ID
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartServiceException("Item not found in user's cart", HttpStatus.NOT_FOUND));

        int updatedQuantity = item.getQuantity() + amount;

        // Check inventory only if increasing
        if (amount > 0) {
            String productId = item.getProductId();

            logger.info("Calling Inventory service for productId: {}", productId);
            // Fetch inventory from external Inventory service
            InventoryResponseDTO inventory = interServiceCall.getInventoryByProductId(productId, token);
            // Check if inventory is sufficient
            if (inventory == null || inventory.getAvailableQuantity() < updatedQuantity) {
                logger.warn("Insufficient stock for product: {}. Available: {}, Required: {}",
                        productId, inventory != null ? inventory.getAvailableQuantity() : 0, updatedQuantity);
                throw new CartServiceException("Insufficient stock for product: " + productId, HttpStatus.BAD_REQUEST);
            }
        }
        // If updated quantity is zero or negative, remove the item from the cart
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

    public ApiResponseDTO removeItemFromCart(JwtAuthenticationToken principal, String itemId) {
        String userKeycloakId = principal.getToken().getSubject();
        logger.info("Removing item with productId: {} from cart for user: {}", itemId, userKeycloakId);

        try {
            Cart cart = cartRepository.findByUserKeycloakId(userKeycloakId)
                    .orElseThrow(() -> new CartServiceException("Cart not found for user: " + userKeycloakId, HttpStatus.NOT_FOUND));

            boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(itemId));

            if (!removed) {
                logger.warn("Item with productId {} not found in cart for user {}", itemId, userKeycloakId);
                throw new CartServiceException("Item not found in cart", HttpStatus.NOT_FOUND);
            }

            Cart updatedCart = cartRepository.save(cart);
            logger.info("Item successfully removed from cart for user: {}", userKeycloakId);
            return new ApiResponseDTO("success", "Item removed from cart successfully", updatedCart);

        } catch (CartServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while removing item from cart", e);
            throw new CartServiceException("Unable to remove item from cart", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponseDTO deleteCart(JwtAuthenticationToken principal) {
        String userKeycloakId = principal.getToken().getSubject();
        logger.info("Attempting to delete cart for userKeycloakId: {}", userKeycloakId);

        try {
            Optional<Cart> cartOpt = cartRepository.findByUserKeycloakId(userKeycloakId);
            if (cartOpt.isPresent()) {
                cartRepository.delete(cartOpt.get());
                logger.info("Cart successfully deleted for user: {}", userKeycloakId);
                return new ApiResponseDTO("success", "Cart deleted successfully", null);
            } else {
                logger.warn("Cart not found for user: {}", userKeycloakId);
                throw new CartServiceException("Cart not found for user: " + userKeycloakId, HttpStatus.NOT_FOUND);
            }
        } catch (CartServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error while deleting cart for user: {}", userKeycloakId, e);
            throw new CartServiceException("Unable to delete cart", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponseDTO getCartSummary(JwtAuthenticationToken principal, double discountPercent) {
        String userKeycloakId = principal.getToken().getSubject();
        logger.info("Fetching cart summary for user: {}, discountPercent: {}", userKeycloakId, discountPercent);

        try {
            Cart cart = cartRepository.findByUserKeycloakId(userKeycloakId)
                    .orElseThrow(() -> new CartServiceException("Cart not found for user: " + userKeycloakId, HttpStatus.NOT_FOUND));

            int totalItems = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
            double totalPrice = cart.getItems().stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();

            double discount = (discountPercent / 100.0) * totalPrice;
            double finalAmount = totalPrice - discount;

            CartSummaryDTO summary = new CartSummaryDTO(totalItems, totalPrice, discount, finalAmount);
            logger.info("Cart summary calculated successfully for user: {}", userKeycloakId);

            return new ApiResponseDTO("success", "Cart summary fetched successfully", summary);

        } catch (CartServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error while calculating cart summary for user: {}", userKeycloakId, e);
            throw new CartServiceException("Unable to fetch cart summary", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
