package com.ecommerce.user_service.service;


import com.ecommerce.user_service.dto.request.UserRequestDto;
import com.ecommerce.user_service.dto.response.ApiResponseDto;
import com.ecommerce.user_service.exception.UserServiceException;
import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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
@Slf4j
public class UserService {

    @Autowired
    private Keycloak keycloakAdminClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${keycloak.realm}")
    private String realm;

    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    public ApiResponseDto registerCustomer(@Valid UserRequestDto request, String keycloakUserId) {
        try {
                // **Encrypt the password before saving it**
                String encryptedPassword = passwordEncoder.encode(request.getPassword());
                LOGGER.info("Encrypted password generated successfully for user: " + request.getUsername());

                // Save user details in the local database
                User newUser = new User();
                newUser.setUsername(request.getUsername());
                newUser.setEmail(request.getEmail());
                newUser.setPassword(encryptedPassword);
                newUser.setPhoneNumber(request.getPhoneNumber());
                newUser.setFirstName(request.getFirstName());
                newUser.setLastName(request.getLastName());
                newUser.setCountry(request.getCountry());
                newUser.setAddressLine(request.getAddressLine());
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setUpdatedAt(LocalDateTime.now());

                // Store Keycloak ID in local database
                newUser.setKeycloakId(keycloakUserId);

                // Save user in repository
                userRepository.save(newUser);
                LOGGER.info("User '" + request.getUsername() + "' details saved in the database");

                return new ApiResponseDto("success", "User created successfully", keycloakUserId);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during registration for username '" + request.getUsername() + "': " + e.getMessage(), e);
            throw new UserServiceException("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public ApiResponseDto updateProfile(UserRequestDto request, String userId) {
        try {

            // ================= Update User in MySQL =================
            // Retrieve the user from MySQL by Keycloak ID
            User user = userRepository.findByKeycloakId(userId)
                    .orElseThrow(() -> new UserServiceException("User not found", HttpStatus.NOT_FOUND));

            // Update the user's information in the database
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setCountry(request.getCountry());
            user.setAddressLine(request.getAddressLine());
            user.setUpdatedAt(LocalDateTime.now());

            // Save the updated user details in MySQL
            userRepository.save(user);
            LOGGER.info("User details updated in MySQL for user ID: " + userId);

            return new ApiResponseDto("success", "Profile updated successfully", null);
        } catch (Exception e) {
            // Log and throw a custom exception if any failure occurs
            LOGGER.severe("Profile update failed: " + e.getMessage());
            throw new UserServiceException("Failed to update profile", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    public ApiResponseDto getAllUsers() {
        try {
            LOGGER.info("Fetching all users from database.");

            // Fetch all users from MySQL
            List<User> users = userRepository.findAll();

            // If no users are found, return an error response
            if (users.isEmpty()) {
                LOGGER.warning("No users found.");
                return new ApiResponseDto("error", "No users found", Collections.emptyList());
            }

            LOGGER.info("Successfully fetched all users.");
            return new ApiResponseDto("success", "Users retrieved successfully", users);
        } catch (Exception e) {
            // Log and throw a custom exception in case of failure
            LOGGER.severe("Error fetching users: " + e.getMessage());
            throw new UserServiceException("Failed to fetch users", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public ApiResponseDto getUserByToken(String keycloakId) {
        try {

            // Fetch user details from MySQL
            User user = userRepository.findByKeycloakId(keycloakId)
                    .orElseThrow(() -> new UserServiceException("User not found", HttpStatus.NOT_FOUND));

            // Map user details for response
            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("id", user.getId());
            userDetails.put("keycloakId", user.getKeycloakId());
            userDetails.put("username", user.getUsername());
            userDetails.put("email", user.getEmail());
            userDetails.put("firstName", user.getFirstName());
            userDetails.put("lastName", user.getLastName());
            userDetails.put("phoneNumber", user.getPhoneNumber());
            userDetails.put("country", user.getCountry());
            userDetails.put("addressLine", user.getAddressLine());
            userDetails.put("createdAt", user.getCreatedAt());
            userDetails.put("updatedAt", user.getUpdatedAt());

            LOGGER.info("User details retrieved successfully for Keycloak ID: " + keycloakId);
            return new ApiResponseDto("success", "User retrieved successfully", userDetails);
        } catch (UserServiceException e) {
            // Propagate known exceptions
            throw e;
        } catch (Exception e) {
            // Log and throw a generic error
            LOGGER.severe("Error fetching user details: " + e.getMessage());
            throw new UserServiceException("Failed to fetch user details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}



