package com.nebula.studio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nebula.studio.common.BusinessException;
import com.nebula.studio.dto.request.CreateStyleRequest;
import com.nebula.studio.dto.request.UpdateStyleRequest;
import com.nebula.studio.entity.Style;
import com.nebula.studio.mapper.StyleMapper;
import com.nebula.studio.service.StyleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class StyleServiceImpl extends ServiceImpl<StyleMapper, Style> implements StyleService {

    @Override
    public List<Style> listActiveStyles() {
        return lambdaQuery().eq(Style::getStatus, "ACTIVE").orderByDesc(Style::getCreatedAt).list();
    }

    @Override
    public IPage<Style> listStyles(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<Style> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Style::getName, keyword).or().like(Style::getDescription, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Style::getStatus, status);
        }
        wrapper.orderByDesc(Style::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Style createStyle(Long userId, CreateStyleRequest request) {
        if (lambdaQuery().eq(Style::getName, request.getName()).exists()) {
            throw new BusinessException(2002, "风格名称已存在");
        }
        Style style = new Style();
        style.setDesignerId(userId);
        style.setName(request.getName());
        style.setDescription(request.getDescription());
        style.setPreviewUrl(request.getPreviewUrl());
        style.setConfig(request.getConfig());
        style.setStatus("ACTIVE");
        style.setCreatedBy(userId);
        style.setUpdatedBy(userId);
        save(style);
        return style;
    }

    @Override
    public Style updateStyle(Long id, Long userId, UpdateStyleRequest request) {
        Style style = getById(id);
        if (style == null) {
            throw new BusinessException(2001, "风格不存在");
        }
        if (StringUtils.hasText(request.getName())) {
            style.setName(request.getName());
        }
        if (request.getDescription() != null) {
            style.setDescription(request.getDescription());
        }
        if (request.getPreviewUrl() != null) {
            style.setPreviewUrl(request.getPreviewUrl());
        }
        if (request.getConfig() != null) {
            style.setConfig(request.getConfig());
        }
        if (StringUtils.hasText(request.getStatus())) {
            style.setStatus(request.getStatus());
        }
        style.setUpdatedBy(userId);
        updateById(style);
        return style;
    }

    @Override
    public void deleteStyle(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(2001, "风格不存在");
        }
    }
}
