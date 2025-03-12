package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.request.UserRequestDto;
import com.ecommerce.user_service.dto.response.ApiResponseDto;
import com.ecommerce.user_service.exception.UserServiceException;
import com.ecommerce.user_service.model.Seller;
import com.ecommerce.user_service.repository.SellerRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class SellerService {

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger LOGGER = Logger.getLogger(SellerService.class.getName());

    public ApiResponseDto registerSeller(@Valid UserRequestDto request, String keycloakUserId) {
        try {
            // **Encrypt the password before saving it**
            String encryptedPassword = passwordEncoder.encode(request.getPassword());
            LOGGER.info("Encrypted password generated successfully for seller: " + request.getUsername());


            // Save user details in the local database
            Seller newSeller = new Seller();
            newSeller.setUsername(request.getUsername());
            newSeller.setEmail(request.getEmail());
            newSeller.setPassword(encryptedPassword);
            newSeller.setPhoneNumber(request.getPhoneNumber());
            newSeller.setFirstName(request.getFirstName());
            newSeller.setLastName(request.getLastName());
            newSeller.setCountry(request.getCountry());
            newSeller.setStoreAddress(request.getAddressLine());
            newSeller.setBusinessName(request.getBusinessName());
            newSeller.setGstNumber(request.getGstNumber());
            newSeller.setCreatedAt(LocalDateTime.now());
            newSeller.setUpdatedAt(LocalDateTime.now());

            // Store Keycloak ID in local database
            newSeller.setKeycloakId(keycloakUserId);

            // Save user in repository
            sellerRepository.save(newSeller);
            LOGGER.info("Seller '" + request.getUsername() + "' details saved in the database");


            return new ApiResponseDto("success", "Seller created successfully", keycloakUserId);

        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during registration for username '" + request.getUsername() + "': " + e.getMessage(), e);
            throw new UserServiceException("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }

    public ApiResponseDto updateProfile(UserRequestDto request, String sellerId) {
        try {

            // ================= Update Seller in MySQL =================
            // Retrieve the seller from MySQL by Keycloak ID
            Seller seller = sellerRepository.findByKeycloakId(sellerId)
                    .orElseThrow(() -> new UserServiceException("Seller not found", HttpStatus.NOT_FOUND));

            // Update the user's information in the database
            seller.setFirstName(request.getFirstName());
            seller.setLastName(request.getLastName());
            seller.setBusinessName(request.getBusinessName());
            seller.setGstNumber(request.getGstNumber());
            seller.setEmail(request.getEmail());
            seller.setPhoneNumber(request.getPhoneNumber());
            seller.setCountry(request.getCountry());
            seller.setStoreAddress(request.getAddressLine());
            seller.setUpdatedAt(LocalDateTime.now());

            // Save the updated seller details in MySQL
            sellerRepository.save(seller);
            LOGGER.info("Seller details updated in MySQL for seller ID: " + sellerId);

            return new ApiResponseDto("success", "Profile updated successfully", null);
        } catch (Exception e) {
            // Log and throw a custom exception if any failure occurs
            LOGGER.severe("Profile update failed: " + e.getMessage());
            throw new UserServiceException("Failed to update profile", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponseDto getUserByToken(String keycloakId) {
        try {

            // Fetch seller details from MySQL
            Seller seller = sellerRepository.findByKeycloakId(keycloakId)
                    .orElseThrow(() -> new UserServiceException("User not found", HttpStatus.NOT_FOUND));

            // Map user details for response
            Map<String, Object> sellerDetails = new HashMap<>();
            sellerDetails.put("id", seller.getId());
            sellerDetails.put("keycloakId", seller.getKeycloakId());
            sellerDetails.put("username", seller.getUsername());
            sellerDetails.put("email", seller.getEmail());
            sellerDetails.put("firstName", seller.getFirstName());
            sellerDetails.put("lastName", seller.getLastName());
            sellerDetails.put("phoneNumber", seller.getPhoneNumber());
            sellerDetails.put("country", seller.getCountry());
            sellerDetails.put("addressLine", seller.getStoreAddress());
            sellerDetails.put("buisnessName", seller.getBusinessName());
            sellerDetails.put("gstNumber", seller.getGstNumber());
            sellerDetails.put("createdAt", seller.getCreatedAt());
            sellerDetails.put("updatedAt", seller.getUpdatedAt());

            LOGGER.info("Seller details retrieved successfully for Keycloak ID: " + keycloakId);
            return new ApiResponseDto("success", "Seller retrieved successfully", sellerDetails);
        } catch (UserServiceException e) {
            // Propagate known exceptions
            throw e;
        } catch (Exception e) {
            // Log and throw a generic error
            LOGGER.severe("Error fetching seller details: " + e.getMessage());
            throw new UserServiceException("Failed to fetch seller details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponseDto getAllSellers() {
        try {
            LOGGER.info("Fetching all sellers from database.");

            // Fetch all sellers from MySQL
            List<Seller> sellers = sellerRepository.findAll();

            // If no sellers are found, return an error response
            if (sellers.isEmpty()) {
                LOGGER.warning("No sellers found.");
                return new ApiResponseDto("error", "No sellers found", Collections.emptyList());
            }

            LOGGER.info("Successfully fetched all sellers.");
            return new ApiResponseDto("success", "Sellers retrieved successfully", sellers);
        } catch (Exception e) {
            // Log and throw a custom exception in case of failure
            LOGGER.severe("Error fetching sellers: " + e.getMessage());
            throw new UserServiceException("Failed to fetch sellers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
