package com.nebula.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nebula.studio.dto.request.CreateStyleRequest;
import com.nebula.studio.dto.request.UpdateStyleRequest;
import com.nebula.studio.entity.Style;

import java.util.List;

public interface StyleService extends IService<Style> {

    List<Style> listActiveStyles();

    IPage<Style> listStyles(int page, int size, String keyword, String status);

    Style createStyle(Long userId, CreateStyleRequest request);

    Style updateStyle(Long id, Long userId, UpdateStyleRequest request);

    void deleteStyle(Long id);
}
