package com.nebula.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nebula.studio.dto.request.Text2ImageRequest;
import com.nebula.studio.entity.ImageRecord;
import org.springframework.web.multipart.MultipartFile;

public interface ImageRecordService extends IService<ImageRecord> {

    ImageRecord text2Image(Long userId, Text2ImageRequest request);

    ImageRecord image2Text(Long userId, MultipartFile image);

    IPage<ImageRecord> listHistory(Long userId, String type, int page, int size);

    ImageRecord getByIdAndUser(Long id, Long userId);

    ImageRecord updateRecord(Long id, Long userId, ImageRecord request);

    void deleteRecord(Long id, Long userId);
}
