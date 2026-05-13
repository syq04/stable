package com.nebula.studio.service.impl;

import com.nebula.studio.common.BusinessException;
import com.nebula.studio.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    @Value("${storage.local.path:./uploads}")
    private String storagePath;

    @Value("${storage.local.url-prefix:/uploads/}")
    private String urlPrefix;

    @Override
    public String store(byte[] data, String subDir, String filename) {
        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path dir = Paths.get(storagePath, subDir, dateDir);
            Files.createDirectories(dir);

            String ext = "";
            int dotIdx = filename.lastIndexOf('.');
            if (dotIdx > 0) {
                ext = filename.substring(dotIdx).toLowerCase();
            }
            String uniqueName = UUID.randomUUID().toString().replace("-", "") + ext;

            Path filePath = dir.resolve(uniqueName);
            Files.write(filePath, data);

            String relativePath = subDir + "/" + dateDir + "/" + uniqueName;
            log.info("文件已存储: {}", filePath.toAbsolutePath());
            return urlPrefix + relativePath;
        } catch (IOException e) {
            throw new BusinessException("文件存储失败: " + e.getMessage());
        }
    }

    @Override
    public String store(MultipartFile file, String subDir) {
        try {
            String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
            return store(file.getBytes(), subDir, originalFilename);
        } catch (IOException e) {
            throw new BusinessException("文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] load(String filePath) {
        try {
            String relativePath = filePath;
            if (filePath.startsWith(urlPrefix)) {
                relativePath = filePath.substring(urlPrefix.length());
            }
            Path path = Paths.get(storagePath, relativePath);
            if (!Files.exists(path)) {
                throw new BusinessException("文件不存在: " + relativePath);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BusinessException("文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            String relativePath = filePath;
            if (filePath.startsWith(urlPrefix)) {
                relativePath = filePath.substring(urlPrefix.length());
            }
            Path path = Paths.get(storagePath, relativePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", e.getMessage());
        }
    }

    @Override
    public String getUrlPrefix() {
        return urlPrefix;
    }
}
