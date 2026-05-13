package com.nebula.studio.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String email;
    private String role;
    private String avatarUrl;

    public LoginResponse(String token, Long userId, String username, String email, String role, String avatarUrl) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }
}
