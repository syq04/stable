package com.nebula.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nebula.studio.common.Result;
import com.nebula.studio.dto.request.CreateTrainingTaskRequest;
import com.nebula.studio.entity.TrainingTask;
import com.nebula.studio.security.JwtUserDetails;
import com.nebula.studio.service.TrainingTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lora")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingTaskService trainingTaskService;

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    public Result<IPage<TrainingTask>> listTasks(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(trainingTaskService.listTasks(userDetails.getUserId(), status, page, size));
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    public Result<TrainingTask> getTaskDetail(@AuthenticationPrincipal JwtUserDetails userDetails,
                                              @PathVariable Long id) {
        return Result.success(trainingTaskService.getTaskDetail(id, userDetails.getUserId()));
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    public Result<TrainingTask> createTask(@AuthenticationPrincipal JwtUserDetails userDetails,
                                           @Valid @RequestBody CreateTrainingTaskRequest request) {
        return Result.success(trainingTaskService.createTask(userDetails.getUserId(), request));
    }

    @PutMapping("/tasks/{id}/start")
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    public Result<TrainingTask> startTask(@AuthenticationPrincipal JwtUserDetails userDetails,
                                          @PathVariable Long id) {
        return Result.success(trainingTaskService.startTask(id, userDetails.getUserId()));
    }

    @GetMapping("/tasks/{id}/logs")
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    public Result<TrainingTask> getTaskLogs(@AuthenticationPrincipal JwtUserDetails userDetails,
                                            @PathVariable Long id) {
        return Result.success(trainingTaskService.getTaskDetail(id, userDetails.getUserId()));
    }

    @GetMapping(value = "/tasks/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    public Result<TrainingTask> downloadModel(@AuthenticationPrincipal JwtUserDetails userDetails,
                                               @PathVariable Long id) {
        return Result.success(trainingTaskService.getTaskDetail(id, userDetails.getUserId()));
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    public Result<Void> deleteTask(@AuthenticationPrincipal JwtUserDetails userDetails,
                                   @PathVariable Long id) {
        trainingTaskService.deleteTask(id, userDetails.getUserId());
        return Result.success();
    }
}
