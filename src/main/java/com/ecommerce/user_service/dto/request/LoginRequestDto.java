package com.ecommerce.user_service.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDto {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    public @NotBlank(message = "Email cannot be blank") String getUsername() {
        return username;
    }

    public void setEmail(@NotBlank(message = "Email cannot be blank") String username) {
        this.username = username;
    }

    public @NotBlank(message = "Password cannot be blank") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Password cannot be blank") String password) {
        this.password = password;
    }
}