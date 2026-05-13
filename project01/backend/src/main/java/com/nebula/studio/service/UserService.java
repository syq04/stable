package com.nebula.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nebula.studio.dto.request.*;
import com.nebula.studio.dto.response.LoginResponse;
import com.nebula.studio.entity.User;

public interface UserService extends IService<User> {

    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    User getProfile(Long userId);

    User updateProfile(Long userId, UpdateUserRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    IPage<User> listUsers(int page, int size, String keyword, String role);

    User createUser(CreateUserRequest request);

    User updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}
