package com.nebula.studio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nebula.studio.common.BusinessException;
import com.nebula.studio.dto.request.CreateTrainingTaskRequest;
import com.nebula.studio.entity.TrainingTask;
import com.nebula.studio.mapper.TrainingTaskMapper;
import com.nebula.studio.service.TrainingTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
public class TrainingTaskServiceImpl extends ServiceImpl<TrainingTaskMapper, TrainingTask> implements TrainingTaskService {

    @Override
    public IPage<TrainingTask> listTasks(Long userId, String status, int page, int size) {
        LambdaQueryWrapper<TrainingTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainingTask::getUserId, userId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(TrainingTask::getStatus, status);
        }
        wrapper.orderByDesc(TrainingTask::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public TrainingTask createTask(Long userId, CreateTrainingTaskRequest request) {
        TrainingTask task = new TrainingTask();
        task.setUserId(userId);
        task.setName(request.getName());
        task.setParams(request.getParams());
        task.setDataPath(request.getDataPath());
        task.setStatus("PENDING");
        task.setProgress(0f);
        task.setCreatedBy(userId);
        save(task);
        return task;
    }

    @Override
    public TrainingTask startTask(Long id, Long userId) {
        TrainingTask task = getById(id);
        if (task == null) {
            throw new BusinessException(3001, "训练任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此任务");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("任务状态不允许启动");
        }
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        updateById(task);

        log.info("训练任务已启动: id={}, name={}", task.getId(), task.getName());
        return task;
    }

    @Override
    public TrainingTask cancelTask(Long id, Long userId) {
        TrainingTask task = getById(id);
        if (task == null) {
            throw new BusinessException(3001, "训练任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此任务");
        }
        if ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
            throw new BusinessException("任务已完成，无法取消");
        }
        task.setStatus("FAILED");
        task.setCompletedAt(LocalDateTime.now());
        task.setLogs((task.getLogs() == null ? "" : task.getLogs()) + "\n任务已取消");
        updateById(task);
        return task;
    }

    @Override
    public TrainingTask getTaskDetail(Long id, Long userId) {
        TrainingTask task = getById(id);
        if (task == null) {
            throw new BusinessException(3001, "训练任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此任务");
        }
        return task;
    }

    @Override
    public void deleteTask(Long id, Long userId) {
        TrainingTask task = getById(id);
        if (task == null) {
            throw new BusinessException(3001, "训练任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此任务");
        }
        removeById(id);
    }
}
