package com.nebula.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nebula.studio.common.Result;
import com.nebula.studio.dto.request.CreateUserRequest;
import com.nebula.studio.dto.request.UpdateUserRequest;
import com.nebula.studio.entity.User;
import com.nebula.studio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UserService userService;

    @GetMapping
    public Result<IPage<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        return Result.success(userService.listUsers(page, size, keyword, role));
    }

    @PostMapping
    public Result<User> createUser(@RequestBody CreateUserRequest request) {
        return Result.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable("id") Long id, @RequestBody UpdateUserRequest request) {
        return Result.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return Result.success(null);
    }
}
