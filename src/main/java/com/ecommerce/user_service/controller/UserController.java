package com.ecommerce.user_service.controller;


import com.ecommerce.user_service.dto.request.LoginRequestDto;
import com.ecommerce.user_service.dto.request.UserRequestDto;
import com.ecommerce.user_service.dto.response.ApiResponseDto;
import com.ecommerce.user_service.dto.response.AuthResponseDto;
import com.ecommerce.user_service.service.KeycloakService;
import com.ecommerce.user_service.service.SellerService;
import com.ecommerce.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private KeycloakService keycloakService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto> register(@Valid @RequestBody UserRequestDto request){
        return ResponseEntity.ok(keycloakService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request){
        return ResponseEntity.ok(keycloakService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto> logout(@RequestParam String refreshToken){
        return ResponseEntity.ok(keycloakService.logout(refreshToken));
    }

    @PutMapping("/update-profile")
    public  ResponseEntity<ApiResponseDto> uddateProfile( @RequestBody UserRequestDto request,
                                                          @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(keycloakService.updateProfile(request, token));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/allUsers")
    public ResponseEntity<ApiResponseDto> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/allSellers")
    public ResponseEntity<ApiResponseDto> getAllSellers() {
        return ResponseEntity.ok(sellerService.getAllSellers());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto> getUserByToken(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(keycloakService.getUserByToken(token));
    }


}
