package com.nebula.studio.controller;

import com.nebula.studio.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    /**
     * 上传目录（绝对路径），默认在项目根目录下的 uploads 目录
     */
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 获取上传目录的绝对路径，确保目录存在
     */
    private Path getUploadPath() {
        Path path = Paths.get(uploadDir, "images").toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            log.error("创建上传目录失败: {}", path, e);
        }
        return path;
    }

    /**
     * 上传图片（用于风格预览图等）
     */
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只支持图片文件");
            }

            // 获取上传目录（自动创建）
            Path uploadPath = getUploadPath();

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(filename);

            // 保存文件
            file.transferTo(filePath.toFile());

            // 返回可访问的 URL
            String fileUrl = "/uploads/images/" + filename;
            log.info("图片上传成功: {} -> {}", filePath, fileUrl);
            return Result.success(fileUrl);

        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
