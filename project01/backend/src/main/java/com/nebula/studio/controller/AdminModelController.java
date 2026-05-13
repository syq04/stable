package com.nebula.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nebula.studio.common.Result;
import com.nebula.studio.entity.LoraModel;
import com.nebula.studio.service.LoraModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/system/models")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminModelController {

    private final LoraModelService loraModelService;

    @GetMapping
    public Result<IPage<LoraModel>> listModels(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(loraModelService.listModels(page, size, keyword));
    }

    @PostMapping("/upload")
    public Result<LoraModel> uploadModel(@RequestParam("file") MultipartFile file,
                                         @RequestParam("name") String name,
                                         @RequestParam(value = "version", defaultValue = "1.0.0") String version) {
        return Result.success(loraModelService.uploadModel(file, name, version));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        loraModelService.removeById(id);
        return Result.success();
    }
}
