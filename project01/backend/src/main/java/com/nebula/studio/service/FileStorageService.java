package com.nebula.studio.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {
    String store(byte[] data, String subDir, String filename);
    String store(MultipartFile file, String subDir);
    byte[] load(String filePath);
    void delete(String filePath);
    String getUrlPrefix();
}
