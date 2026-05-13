package com.nebula.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nebula.studio.dto.request.CreateTrainingTaskRequest;
import com.nebula.studio.entity.TrainingTask;

public interface TrainingTaskService extends IService<TrainingTask> {

    IPage<TrainingTask> listTasks(Long userId, String status, int page, int size);

    TrainingTask createTask(Long userId, CreateTrainingTaskRequest request);

    TrainingTask startTask(Long id, Long userId);

    TrainingTask cancelTask(Long id, Long userId);

    TrainingTask getTaskDetail(Long id, Long userId);

    void deleteTask(Long id, Long userId);
}
