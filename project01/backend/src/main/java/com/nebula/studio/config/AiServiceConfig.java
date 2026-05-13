package com.nebula.studio.config;

import com.nebula.studio.ai.ComfyUIClient;
import com.nebula.studio.ai.DoubaoVisionClient;
import com.nebula.studio.ai.StableDiffusionClient;
import com.nebula.studio.service.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AiServiceConfig {

    @Value("${ai.sd.enabled:false}")
    private boolean sdEnabled;

    @Value("${ai.sd.api-url:http://127.0.0.1:7860}")
    private String sdApiUrl;

    @Value("${ai.sd.timeout:120}")
    private int sdTimeout;

    @Value("${ai.doubao.enabled:false}")
    private boolean doubaoEnabled;

    @Value("${ai.doubao.api-url:https://ark.cn-beijing.volces.com/api/v3/chat/completions}")
    private String doubaoApiUrl;

    @Value("${ai.doubao.api-key:}")
    private String doubaoApiKey;

    @Value("${ai.doubao.model:doubao-vision-pro-32k}")
    private String doubaoModel;

    @Value("${ai.doubao.timeout:60}")
    private int doubaoTimeout;

    @Value("${ai.comfyui.enabled:false}")
    private boolean comfyuiEnabled;

    @Value("${ai.comfyui.api-url:http://127.0.0.1:8188}")
    private String comfyuiApiUrl;

    @Value("${ai.comfyui.timeout:120}")
    private int comfyuiTimeout;

    @Value("${ai.comfyui.poll-interval:2000}")
    private int comfyuiPollInterval;

    @Value("${ai.comfyui.workflow:workflow/comfyui_default.json}")
    private String comfyuiWorkflow;

    @Bean
    public ComfyUIClient comfyUIClient(DynamicConfigService dynamicConfigService) {
        ComfyUIClient client = new ComfyUIClient(comfyuiApiUrl, comfyuiTimeout,
                comfyuiPollInterval, comfyuiWorkflow, dynamicConfigService);
        if (comfyuiEnabled) {
            boolean available = client.isAvailable();
            log.info("ComfyUI API [{}], 可用: {}", comfyuiApiUrl, available);
        } else {
            log.info("ComfyUI API 已禁用");
        }
        return client;
    }

    @Bean
    public StableDiffusionClient stableDiffusionClient() {
        StableDiffusionClient client = new StableDiffusionClient(sdApiUrl, sdTimeout);
        if (sdEnabled) {
            boolean available = client.isAvailable();
            log.info("Stable Diffusion API [{}], 可用: {}", sdApiUrl, available);
        } else {
            log.info("Stable Diffusion API 已禁用");
        }
        return client;
    }

    @Bean
    public DoubaoVisionClient doubaoVisionClient() {
        DoubaoVisionClient client = new DoubaoVisionClient(doubaoApiUrl, doubaoApiKey, doubaoModel, doubaoTimeout);
        if (doubaoEnabled) {
            boolean available = client.isAvailable();
            log.info("豆包视觉大模型 API [{}], model={}, 可用: {}", doubaoApiUrl, doubaoModel, available);
        } else {
            log.info("豆包视觉大模型 API 已禁用");
        }
        return client;
    }
}
