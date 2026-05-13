package com.nebula.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nebula.studio.common.Result;
import com.nebula.studio.dto.request.CreateStyleRequest;
import com.nebula.studio.dto.request.UpdateStyleRequest;
import com.nebula.studio.entity.Style;
import com.nebula.studio.security.JwtUserDetails;
import com.nebula.studio.service.StyleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/styles")
@RequiredArgsConstructor
public class StyleController {

    private final StyleService styleService;

    @GetMapping("/active")
    public Result<List<Style>> listActiveStyles() {
        return Result.success(styleService.listActiveStyles());
    }

    @GetMapping
    public Result<IPage<Style>> listStyles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(styleService.listStyles(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<Style> getStyle(@PathVariable Long id) {
        return Result.success(styleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    public Result<Style> createStyle(@AuthenticationPrincipal JwtUserDetails userDetails,
                                     @RequestBody CreateStyleRequest request) {
        return Result.success(styleService.createStyle(userDetails.getUserId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    public Result<Style> updateStyle(@AuthenticationPrincipal JwtUserDetails userDetails,
                                     @PathVariable Long id,
                                     @RequestBody UpdateStyleRequest request) {
        return Result.success(styleService.updateStyle(id, userDetails.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteStyle(@PathVariable Long id) {
        styleService.deleteStyle(id);
        return Result.success();
    }
}
