package com.nebula.studio.ai.provider;

import com.nebula.studio.ai.ComfyUIClient;
import com.nebula.studio.dto.request.Text2ImageRequest;
import com.nebula.studio.service.DynamicConfigService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ComfyUIText2ImageProvider implements Text2ImageProvider {

    private final ComfyUIClient comfyUIClient;
    private final DynamicConfigService dynamicConfigService;

    @Value("${ai.comfyui.enabled:false}")
    private boolean comfyuiEnabled;

    public ComfyUIText2ImageProvider(ComfyUIClient comfyUIClient,
                                     DynamicConfigService dynamicConfigService) {
        this.comfyUIClient = comfyUIClient;
        this.dynamicConfigService = dynamicConfigService;
    }

    @PostConstruct
    public void init() {
        dynamicConfigService.registerDefault("ai.comfyui.api-url",
                comfyUIClient.getEffectiveApiUrl());
    }

    @Override
    public String getProviderName() {
        return "comfyui";
    }

    @Override
    public boolean isAvailable() {
        return comfyuiEnabled && comfyUIClient.isAvailable();
    }

    @Override
    public Text2ImageResult generate(Text2ImageRequest request, String builtPrompt) {
        try {
            int width = request.getWidth() != null ? request.getWidth() : 512;
            int height = request.getHeight() != null ? request.getHeight() : 512;
            int steps = request.getSteps() != null ? request.getSteps() : 20;
            double cfg = request.getCfgScale() != null ? request.getCfgScale() : 8.0;
            Long seed = request.getSeed();

            byte[] imageData = comfyUIClient.generate(
                    builtPrompt, request.getNegativePrompt(),
                    width, height, steps, cfg, seed,
                    request.getSamplerName());

            Text2ImageResult result = Text2ImageResult.success(imageData, getProviderName());
            if (seed != null) {
                result.setSeed(seed);
            } else {
                result.setSeed(System.currentTimeMillis() % Integer.MAX_VALUE);
            }
            return result;
        } catch (Exception e) {
            log.error("ComfyUI 文生图调用失败: {}", e.getMessage());
            return Text2ImageResult.fail("调用失败: " + e.getMessage(), getProviderName());
        }
    }
}
