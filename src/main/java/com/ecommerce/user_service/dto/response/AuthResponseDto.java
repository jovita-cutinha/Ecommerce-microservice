package com.ecommerce.user_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;

    public AuthResponseDto(String accessToken, String refreshToken, Long expiresIn ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }


}