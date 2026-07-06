package com.nebula.studio.controller;

import com.nebula.studio.ai.provider.LocalModelText2ImageProvider;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nebula.studio.common.Result;
import com.nebula.studio.dto.request.Text2ImageRequest;
import com.nebula.studio.entity.ImageRecord;
import com.nebula.studio.security.JwtUserDetails;
import com.nebula.studio.service.ImageRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ImageController {

    private final ImageRecordService imageRecordService;
    private final LocalModelText2ImageProvider localModelProvider;

    @PostMapping("/text2image/generate")
    public Result<ImageRecord> text2Image(@AuthenticationPrincipal JwtUserDetails userDetails,
                                          @Valid @RequestBody Text2ImageRequest request) {
        return Result.success(imageRecordService.text2Image(userDetails.getUserId(), request));
    }

    @PostMapping("/text2image/evaluate")
    public Result<Map<String, Object>> evaluateText2Image(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                           @Valid @RequestBody Text2ImageRequest request) {
        return Result.success(imageRecordService.evaluate(userDetails.getUserId(), request));
    }

    @GetMapping("/text2image/progress/{taskId}")
    public Result<Map<String, Object>> getText2ImageProgress(@PathVariable String taskId) {
        return Result.success(localModelProvider.getProgress(taskId));
    }

    @GetMapping("/text2image/history")
    public Result<IPage<ImageRecord>> text2ImageHistory(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(imageRecordService.listHistory(userDetails.getUserId(), "TEXT2IMAGE", page, size));
    }

    @GetMapping("/text2image/{id}")
    public Result<ImageRecord> getText2ImageDetail(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                   @PathVariable Long id) {
        return Result.success(imageRecordService.getByIdAndUser(id, userDetails.getUserId()));
    }

    @DeleteMapping("/text2image/{id}")
    public Result<Void> deleteText2ImageRecord(@AuthenticationPrincipal JwtUserDetails userDetails,
                                               @PathVariable Long id) {
        imageRecordService.deleteRecord(id, userDetails.getUserId());
        return Result.success();
    }

    @PostMapping("/image2text/analyze")
    public Result<ImageRecord> image2Text(@AuthenticationPrincipal JwtUserDetails userDetails,
                                          @RequestParam(value = "image", required = false) MultipartFile image,
                                          @RequestParam(value = "analysisType", defaultValue = "general") String analysisType) {
        if (image == null || image.isEmpty()) {
            return Result.error(400, "请上传图片文件");
        }
        return Result.success(imageRecordService.image2Text(userDetails.getUserId(), image, analysisType));
    }

    @GetMapping("/image2text/history")
    public Result<IPage<ImageRecord>> image2TextHistory(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(imageRecordService.listHistory(userDetails.getUserId(), "IMAGE2TEXT", page, size));
    }

    @GetMapping("/image2text/{id}")
    public Result<ImageRecord> getImage2TextDetail(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                   @PathVariable Long id) {
        return Result.success(imageRecordService.getByIdAndUser(id, userDetails.getUserId()));
    }

    @PutMapping("/image2text/{id}")
    public Result<ImageRecord> updateImage2TextResult(@AuthenticationPrincipal JwtUserDetails userDetails,
                                                      @PathVariable Long id,
                                                      @RequestBody Map<String, String> request) {
        ImageRecord safeRecord = new ImageRecord();
        safeRecord.setOutputContent(request.get("outputContent"));
        return Result.success(imageRecordService.updateRecord(id, userDetails.getUserId(), safeRecord));
    }

    @DeleteMapping("/image2text/{id}")
    public Result<Void> deleteImage2TextRecord(@AuthenticationPrincipal JwtUserDetails userDetails,
                                               @PathVariable Long id) {
        imageRecordService.deleteRecord(id, userDetails.getUserId());
        return Result.success();
    }
}
