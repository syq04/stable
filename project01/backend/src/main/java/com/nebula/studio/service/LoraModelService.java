package com.nebula.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nebula.studio.entity.LoraModel;
import org.springframework.web.multipart.MultipartFile;

public interface LoraModelService extends IService<LoraModel> {

    IPage<LoraModel> listModels(int page, int size, String keyword);

    LoraModel uploadModel(MultipartFile file, String name, String version);
}
