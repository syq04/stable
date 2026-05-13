package com.nebula.studio.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtUserDetails {
    private Long userId;
    private String username;
    private String role;
}
