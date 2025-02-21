package com.ecommerce.user_service.service;


import com.ecommerce.user_service.dto.request.LoginRequestDto;
import com.ecommerce.user_service.dto.request.UserRequestDto;
import com.ecommerce.user_service.dto.response.ApiResponseDto;
import com.ecommerce.user_service.dto.response.AuthResponseDto;
import com.ecommerce.user_service.exception.UserServiceException;
import com.ecommerce.user_service.model.Role;
import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    private static final String DEFAULT_ROLE = "CUSTOMER";

    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    public ApiResponseDto register(@Valid UserRequestDto request) {

        try {

            LOGGER.info("Starting user registration for username: "+request.getUsername());

            // Get the realm resource
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Check if user already exists
            List<UserRepresentation> existingUsers = usersResource.search(request.getUsername(), true);

            if (!existingUsers.isEmpty()) {
                LOGGER.warning("User registration failed: Username '" + request.getUsername() + "' already exists");
                throw new UserServiceException("User with username '" + request.getUsername() + "' already exists", HttpStatus.CONFLICT);
            }
            // Create a user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(true); // Enable the user

            // Set credentials
            CredentialRepresentation credentials = new CredentialRepresentation();
            credentials.setType(CredentialRepresentation.PASSWORD);
            credentials.setValue(request.getPassword());
            credentials.setTemporary(false); // Password is not temporary
            user.setCredentials(Collections.singletonList(credentials));

            // Create the user in Keycloak
            Response response = usersResource.create(user);

            if (response.getStatus() == 201) { // 201 = Created
                // Extract the Keycloak-generated user ID
                String keycloakUserId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                LOGGER.info("User '" + request.getUsername() + "' created successfully in Keycloak with ID: " + keycloakUserId);

                // Assign the default CUSTOMER role
                assignRoleToUser(keycloakUserId, clientId, DEFAULT_ROLE);

                // Save user details in the local database
                User newUser = new User();
                newUser.setUsername(request.getUsername());
                newUser.setEmail(request.getEmail());
                newUser.setPassword(request.getPassword()); // Store encrypted password in production
                newUser.setPhoneNumber(request.getPhoneNumber());
                newUser.setFirstName(request.getFirstName());
                newUser.setLastName(request.getLastName());
                newUser.setCountry(request.getCountry());
                newUser.setAddressLine(request.getAddressLine());
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setUpdatedAt(LocalDateTime.now());
                newUser.setRole(Role.CUSTOMER);

                // Store Keycloak ID in local database
                newUser.setId(keycloakUserId);

                // Save user in repository
                userRepository.save(newUser);
                LOGGER.info("User '" + request.getUsername() + "' details saved in the database");

                return new ApiResponseDto("success", "User created successfully", keycloakUserId);
            } else {
                LOGGER.severe("Failed to create user '" + request.getUsername() + "' in Keycloak: " + response.getStatusInfo().getReasonPhrase());
                throw new UserServiceException("Failed to create user in Keycloak: " + response.getStatusInfo().getReasonPhrase(), HttpStatus.BAD_REQUEST);
            }
        } catch (UserServiceException e) {
            throw e; // Keep the original message for user exists or Keycloak error
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during registration for username '" + request.getUsername() + "': " + e.getMessage(), e);
            throw new UserServiceException("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private void assignRoleToUser(String keycloakUserId, String clientId,String roleName) {
        RealmResource realmResource = keycloakAdminClient.realm(realm);
        try {
            // Fetch the client by its clientId
            ClientRepresentation client = realmResource.clients().findByClientId(clientId).get(0);

            // Get the client role by its name
            RoleRepresentation role = realmResource.clients().get(client.getId()).roles().get(roleName).toRepresentation();

            // Assign the client role to the user
            realmResource.users().get(keycloakUserId).roles().clientLevel(client.getId()).add(Collections.singletonList(role));

            LOGGER.info("Assigned role '" + roleName + "' to user with ID: " + keycloakUserId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error assigning role '" + roleName + "' to user ID '" + keycloakUserId + "': " + e.getMessage(), e);
        }
    }

    public AuthResponseDto login(LoginRequestDto requestDto) {
        try {

            LOGGER.info("Attempting login for user: " + requestDto.getUsername());

            Keycloak keycloakInstance = KeycloakBuilder.builder()
                    .serverUrl(authServerUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .grantType(OAuth2Constants.PASSWORD)
                    .username(requestDto.getUsername())
                    .password(requestDto.getPassword())
                    .grantType("password")
                    .build();

            AccessTokenResponse tokenResponse = keycloakInstance.tokenManager().getAccessToken();
            LOGGER.info("User '" + requestDto.getUsername() + "' logged in successfully");

            return new AuthResponseDto(
                    tokenResponse.getToken(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getExpiresIn()
            );
        } catch (Exception e) {
            LOGGER.warning("Login failed for user '" + requestDto.getUsername() + "': " + e.getMessage());
            throw new UserServiceException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
    }

  public ApiResponseDto logout(String refreshToken) {

    try {

      LOGGER.info("Logging out user using refresh token...");
      RestTemplate restTemplate = new RestTemplate();
      String url = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
      requestBody.add("client_id", clientId);
      requestBody.add("client_secret", clientSecret);
      requestBody.add("refresh_token", refreshToken);

      HttpEntity<MultiValueMap<String, String>> requestEntity =
          new HttpEntity<>(requestBody, headers);
      restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

      // Successful logout response
      LOGGER.info("User logged out successfully");
      return new ApiResponseDto("success", "User logged out successfully", null);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Logout failed: " + e.getMessage(), e);
      return new ApiResponseDto("error", "Logout failed", null);
    }
    }
}



