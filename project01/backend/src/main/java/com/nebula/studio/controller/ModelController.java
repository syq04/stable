package com.nebula.studio.controller;

import com.nebula.studio.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/text2image")
@RequiredArgsConstructor
public class ModelController {

    @Value("${ai.local-model.model-dir:../sd-models}")
    private String modelDir;

    @GetMapping("/models")
    public Result<List<Map<String, Object>>> listModels() {
        List<Map<String, Object>> models = new ArrayList<>();
        File dir = new File(modelDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Model directory not found: {}", dir.getAbsolutePath());
            return Result.success(models);
        }
        scanDirectory(dir, models);
        models.sort(Comparator.comparing(m -> (String) m.get("name")));
        return Result.success(models);
    }

    private void scanDirectory(File dir, List<Map<String, Object>> models) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, models);
            } else if (file.getName().toLowerCase().endsWith(".safetensors")) {
                Map<String, Object> model = new LinkedHashMap<>();
                String filename = file.getName();
                String name = filename.substring(0, filename.lastIndexOf('.'));
                model.put("name", name);
                model.put("filename", filename);
                model.put("sizeMb", Math.round(file.length() / (1024.0 * 1024.0) * 100.0) / 100.0);
                model.put("size", file.length());
                models.add(model);
            }
        }
    }
}
