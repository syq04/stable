package com.nebula.studio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nebula.studio.common.BusinessException;
import com.nebula.studio.dto.request.*;
import com.nebula.studio.dto.response.LoginResponse;
import com.nebula.studio.entity.User;
import com.nebula.studio.mapper.UserMapper;
import com.nebula.studio.security.JwtTokenProvider;
import com.nebula.studio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = lambdaQuery().eq(User::getEmail, request.getEmail()).one();
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(1002, "密码错误");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getAvatarUrl());
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (lambdaQuery().eq(User::getUsername, request.getUsername()).exists()) {
            throw new BusinessException(1003, "用户名已存在");
        }
        if (lambdaQuery().eq(User::getEmail, request.getEmail()).exists()) {
            throw new BusinessException(1004, "邮箱已被注册");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setCreatedBy(0L);
        save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getAvatarUrl());
    }

    @Override
    public User getProfile(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        user.setPasswordHash(null);
        return user;
    }

    @Override
    public User updateProfile(Long userId, UpdateUserRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        if (StringUtils.hasText(request.getUsername())) {
            user.setUsername(request.getUsername());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        user.setUpdatedBy(userId);
        updateById(user);
        user.setPasswordHash(null);
        return user;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(1002, "旧密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(userId);
        updateById(user);
    }

    @Override
    public IPage<User> listUsers(int page, int size, String keyword, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getEmail, keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        IPage<User> result = page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPasswordHash(null));
        return result;
    }

    @Override
    public User createUser(CreateUserRequest request) {
        if (lambdaQuery().eq(User::getUsername, request.getUsername()).exists()) {
            throw new BusinessException(1003, "用户名已存在");
        }
        if (lambdaQuery().eq(User::getEmail, request.getEmail()).exists()) {
            throw new BusinessException(1004, "邮箱已被注册");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setCreatedBy(0L);
        save(user);
        user.setPasswordHash(null);
        return user;
    }

    @Override
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        if (StringUtils.hasText(request.getUsername())) {
            user.setUsername(request.getUsername());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getRole())) {
            user.setRole(request.getRole());
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        updateById(user);
        user.setPasswordHash(null);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(1001, "用户不存在");
        }
    }
}
