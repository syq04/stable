package com.nebula.studio.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String username;
    private String email;
    private String role;
    private String avatarUrl;
}
