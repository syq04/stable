package com.nebula.studio.dto.response;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String avatarUrl;
    private String createdAt;
}
