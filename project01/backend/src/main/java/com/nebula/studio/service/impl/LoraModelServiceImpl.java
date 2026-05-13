package com.nebula.studio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nebula.studio.entity.LoraModel;
import com.nebula.studio.mapper.LoraModelMapper;
import com.nebula.studio.service.LoraModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class LoraModelServiceImpl extends ServiceImpl<LoraModelMapper, LoraModel> implements LoraModelService {

    @Override
    public IPage<LoraModel> listModels(int page, int size, String keyword) {
        LambdaQueryWrapper<LoraModel> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(LoraModel::getName, keyword);
        }
        wrapper.orderByDesc(LoraModel::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public LoraModel uploadModel(MultipartFile file, String name, String version) {
        log.info("上传模型文件: {}, name: {}, version: {}", file.getOriginalFilename(), name, version);
        LoraModel model = new LoraModel();
        model.setName(name);
        model.setFilePath("/models/" + file.getOriginalFilename());
        model.setVersion(version);
        model.setCreatedBy(0L);
        save(model);
        return model;
    }
}
