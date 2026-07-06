package com.nebula.studio.controller;

import com.nebula.studio.ai.provider.AiProviderManager;
import com.nebula.studio.common.Result;
import com.nebula.studio.dto.response.AiServiceStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiServiceController {

    private final AiProviderManager providerManager;

    @GetMapping("/status")
    public Result<AiServiceStatusVO> getStatus() {
        AiServiceStatusVO status = new AiServiceStatusVO();

        List<AiProviderManager.ProviderInfo> t2iProviders = providerManager.getText2ImageProviderList();
        List<AiProviderManager.ProviderInfo> i2tProviders = providerManager.getImage2TextProviderList();

        for (AiProviderManager.ProviderInfo info : t2iProviders) {
            AiServiceStatusVO.ServiceInfo si = new AiServiceStatusVO.ServiceInfo();
            si.setAvailable(info.isAvailable());
            si.setActive(info.isActive());
            si.setMode(info.isAvailable() ? "real" : "unavailable");
            si.setMessage(getProviderMessage(info.getName(), info.isAvailable()));
            status.addText2ImageProvider(info.getName(), si);
        }

        for (AiProviderManager.ProviderInfo info : i2tProviders) {
            AiServiceStatusVO.ServiceInfo si = new AiServiceStatusVO.ServiceInfo();
            si.setAvailable(info.isAvailable());
            si.setActive(info.isActive());
            si.setMode(info.isAvailable() ? "real" : "unavailable");
            si.setMessage(getProviderMessage(info.getName(), info.isAvailable()));
            status.addImage2TextProvider(info.getName(), si);
        }

        status.setActiveText2ImageProvider(providerManager.getActiveText2ImageProviderName());
        status.setActiveImage2TextProvider(providerManager.getActiveImage2TextProviderName());

        return Result.success(status);
    }

    @GetMapping("/providers")
    public Result<Map<String, Object>> getProviders() {
        return Result.success(Map.of(
                "text2image", providerManager.getText2ImageProviderList(),
                "image2text", providerManager.getImage2TextProviderList(),
                "activeText2Image", providerManager.getActiveText2ImageProviderName(),
                "activeImage2Text", providerManager.getActiveImage2TextProviderName()
        ));
    }

    @PutMapping("/switch/text2image/{providerName}")
    public Result<String> switchText2ImageProvider(@PathVariable String providerName) {
        try {
            providerManager.switchText2ImageProvider(providerName);
            return Result.success("已切换文生图提供商为: " + providerName);
        } catch (Exception e) {
            return Result.fail("切换失败: " + e.getMessage());
        }
    }

    @PutMapping("/switch/image2text/{providerName}")
    public Result<String> switchImage2TextProvider(@PathVariable String providerName) {
        try {
            providerManager.switchImage2TextProvider(providerName);
            return Result.success("已切换图生文提供商为: " + providerName);
        } catch (Exception e) {
            return Result.fail("切换失败: " + e.getMessage());
        }
    }

    private String getProviderMessage(String name, boolean available) {
        return switch (name) {
            case "local-model" -> available ? "本地模型推理服务已连接" : "本地模型推理服务不可用";
            case "qwen" -> available ? "千问 API 已配置" : "千问 API Key 未配置";
            default -> available ? name + " 服务已连接" : name + " 服务不可用";
        };
    }
}
