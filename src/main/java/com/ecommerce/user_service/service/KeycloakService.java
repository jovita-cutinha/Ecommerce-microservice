package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.request.LoginRequestDto;
import com.ecommerce.user_service.dto.request.UserRequestDto;
import com.ecommerce.user_service.dto.response.ApiResponseDto;
import com.ecommerce.user_service.dto.response.AuthResponseDto;
import com.ecommerce.user_service.exception.UserServiceException;
import com.ecommerce.user_service.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KeycloakService {

    @Autowired
    private Keycloak keycloakAdminClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private SellerService sellerService;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    private static final Logger LOGGER = Logger.getLogger(KeycloakService.class.getName());

    public ApiResponseDto register(@Valid UserRequestDto request) {
        LOGGER.info("Starting user registration for username: " + request.getUsername());

        try {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                LOGGER.warning("User registration failed: Phone number '" + request.getPhoneNumber() + "' already exists");
                throw new UserServiceException("Phone number already exists", HttpStatus.CONFLICT);
            }

            // Get Keycloak realm resource
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Check if username already exists
            List<UserRepresentation> existingUsers = usersResource.search(request.getUsername(), true);
            if (!existingUsers.isEmpty()) {
                LOGGER.warning("User registration failed: Username '" + request.getUsername() + "' already exists");
                throw new UserServiceException("User with username '" + request.getUsername() + "' already exists", HttpStatus.CONFLICT);
            }

            // Create Keycloak User
            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(true);

            // Set Credentials
            CredentialRepresentation credentials = new CredentialRepresentation();
            credentials.setType(CredentialRepresentation.PASSWORD);
            credentials.setValue(request.getPassword());
            credentials.setTemporary(false);
            user.setCredentials(Collections.singletonList(credentials));

            // Create user in Keycloak
            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                // Extract Keycloak user ID
                String keycloakUserId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                LOGGER.info("User '" + request.getUsername() + "' created successfully in Keycloak with ID: " + keycloakUserId);

                // Assign role based on request
                assignRoleToUser(keycloakUserId, clientId, request.getRole());

                // Call the appropriate service based on the role
                switch (request.getRole().toUpperCase()) {
                    case "CUSTOMER":
                        return userService.registerCustomer(request, keycloakUserId);
                    case "SELLER":
                        return sellerService.registerSeller(request, keycloakUserId);
                    default:
                        throw new UserServiceException("Invalid role: " + request.getRole(), HttpStatus.BAD_REQUEST);
                }
            } else {
                LOGGER.severe("Failed to create user '" + request.getUsername() + "' in Keycloak: " + response.getStatusInfo().getReasonPhrase());
                throw new UserServiceException("Failed to create user in Keycloak: " + response.getStatusInfo().getReasonPhrase(), HttpStatus.BAD_REQUEST);
            }
        } catch (UserServiceException e) {
            throw e;
        }
    }

    private void assignRoleToUser(String keycloakUserId, String clientId, String roleName) {
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
            // Log the login attempt with the provided username
            LOGGER.info("Attempting login for user: " + requestDto.getUsername());

            // Build a Keycloak instance to authenticate the user
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

            // Request an access token from Keycloak
            AccessTokenResponse tokenResponse = keycloakInstance.tokenManager().getAccessToken();
            LOGGER.info("User '" + requestDto.getUsername() + "' logged in successfully");

            // Return the authentication response with access token, refresh token, and expiry time
            return new AuthResponseDto(
                    tokenResponse.getToken(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getExpiresIn()
            );

        } catch (Exception e) {
            // Log the failure and return an unauthorized response
            LOGGER.warning("Login failed for user '" + requestDto.getUsername() + "': " + e.getMessage());
            throw new UserServiceException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
    }


    public ApiResponseDto logout(String refreshToken) {
        try {
            // Log the logout attempt
            LOGGER.info("Logging out user using refresh token...");

            // Create a RestTemplate instance for making HTTP requests
            RestTemplate restTemplate = new RestTemplate();

            // Construct the Keycloak logout endpoint URL
            String url = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

            // Set up HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // Specify content type as form data

            // Create the request body with required parameters
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("refresh_token", refreshToken);

            // Wrap request body and headers in an HttpEntity object
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Send the logout request to Keycloak
            restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            // Log successful logout and return response
            LOGGER.info("User logged out successfully");
            return new ApiResponseDto("success", "User logged out successfully", null);

        } catch (Exception e) {
            // Log the error details in case of failure
            LOGGER.log(Level.SEVERE, "Logout failed: " + e.getMessage(), e);
            return new ApiResponseDto("error", "Logout failed", null);
        }
    }

    public ApiResponseDto updateProfile(UserRequestDto request, String token) {
        try {
            String userId = extractUserIdFromToken(token);
            String role = getUserRoleFromToken(token);

            LOGGER.info("Updating profile for User: " + userId + ", Role: " + role);

            // ================= Update User in Keycloak =================
            // Get the realm and user resource from Keycloak
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            // Fetch the current Keycloak user details
            UserRepresentation userRepresentation = userResource.toRepresentation();

            // Update user details in Keycloak
            userRepresentation.setFirstName(request.getFirstName());
            userRepresentation.setLastName(request.getLastName());
            userRepresentation.setEmail(request.getEmail());

            // Persist the updates in Keycloak
            userResource.update(userRepresentation);
            LOGGER.info("User details updated in Keycloak for user ID: " + userId);


            // Redirect based on role
            if ("CUSTOMER".equalsIgnoreCase(role)) {
                return userService.updateProfile(request, userId);
            } else if ("SELLER".equalsIgnoreCase(role)) {
                return sellerService.updateProfile(request, userId);
            } else {
                throw new UserServiceException("Invalid role: " + role, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            LOGGER.severe("Profile update failed: " + e.getMessage());
            throw new UserServiceException("Failed to update profile", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getUserRoleFromToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            String accessToken = token.replace("Bearer ", "");

            // Verify and parse the token
            AccessToken parsedToken = TokenVerifier.create(accessToken, AccessToken.class).getToken();

            // Extract client-level roles from resource_access.ecommerce.roles
            AccessToken.Access resourceAccess = parsedToken.getResourceAccess("ecommerce");

            if (resourceAccess != null) {
                Set<String> roles = resourceAccess.getRoles();

                if (!roles.isEmpty()) {
                    return roles.iterator().next(); //  Get the first role from the Set
                }
            }

            throw new UserServiceException("User has no assigned roles in the ecommerce client", HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            LOGGER.severe("Error extracting user role from token: " + e.getMessage());
            throw new UserServiceException("Unable to extract user role from token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private String extractUserIdFromToken(String token) {
        try {
            // Remove "Bearer " prefix from token
            String accessToken = token.replace("Bearer ", "");

            // Parse the token to extract the subject (Keycloak user ID)
            AccessToken parsedToken = TokenVerifier.create(accessToken, AccessToken.class).getToken();
            return parsedToken.getSubject();
        } catch (VerificationException e) {
            // Log and throw an unauthorized exception if the token is invalid
            LOGGER.severe("Invalid access token: " + e.getMessage());
            throw new UserServiceException("Invalid token", HttpStatus.UNAUTHORIZED);
        }
    }

    public ApiResponseDto getUserByToken(String token) {
        try {
            // Extract Keycloak user ID from the access token
            String keycloakId = extractUserIdFromToken(token);
            String role = getUserRoleFromToken(token);

            LOGGER.info("Fetching user details for Keycloak ID: " + keycloakId + ", Role: " + role);


            // Redirect based on role
            if ("CUSTOMER".equalsIgnoreCase(role)) {
                return userService.getUserByToken(keycloakId);
            } else if ("SELLER".equalsIgnoreCase(role)) {
                return sellerService.getUserByToken(keycloakId);
            } else {
                throw new UserServiceException("Invalid role: " + role, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            LOGGER.severe("Fetching user failed: " + e.getMessage());
            throw new UserServiceException("Failed to fetch user", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
