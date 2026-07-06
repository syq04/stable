package com.nebula.studio.ai.provider;

import com.nebula.studio.dto.request.Text2ImageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AiProviderManager {

    private final List<Text2ImageProvider> text2ImageProviders;
    private final List<Image2TextProvider> image2TextProviders;

    private volatile String activeText2ImageProvider;
    private volatile String activeImage2TextProvider;

    public AiProviderManager(List<Text2ImageProvider> text2ImageProviders,
                             List<Image2TextProvider> image2TextProviders,
                             @Value("${ai.text2image.provider:local-model}") String activeText2ImageProvider,
                             @Value("${ai.image2text.provider:mock}") String activeImage2TextProvider) {
        this.text2ImageProviders = text2ImageProviders;
        this.image2TextProviders = image2TextProviders;
        this.activeText2ImageProvider = activeText2ImageProvider;
        this.activeImage2TextProvider = activeImage2TextProvider;
        log.info("已注册文生图提供商: {}", text2ImageProviders.stream().map(Text2ImageProvider::getProviderName).toList());
        log.info("已注册图生文提供商: {}", image2TextProviders.stream().map(Image2TextProvider::getProviderName).toList());
        log.info("当前激活文生图提供商: {}", activeText2ImageProvider);
        log.info("当前激活图生文提供商: {}", activeImage2TextProvider);
    }

    public Text2ImageProvider getText2ImageProvider() {
        Text2ImageProvider provider = findText2ImageProvider(activeText2ImageProvider);
        if (provider != null && provider.isAvailable()) {
            return provider;
        }
        log.warn("文生图提供商 [{}] 不可用", activeText2ImageProvider);
        return provider;
    }

    public Image2TextProvider getImage2TextProvider() {
        Image2TextProvider provider = findImage2TextProvider(activeImage2TextProvider);
        if (provider != null && provider.isAvailable()) {
            return provider;
        }
        log.warn("图生文提供商 [{}] 不可用", activeImage2TextProvider);
        return provider;
    }

    public void switchText2ImageProvider(String providerName) {
        Text2ImageProvider provider = findText2ImageProvider(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("未知的文生图提供商: " + providerName);
        }
        this.activeText2ImageProvider = providerName;
        log.info("文生图提供商已切换为: {}", providerName);
    }

    public void switchImage2TextProvider(String providerName) {
        Image2TextProvider provider = findImage2TextProvider(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("未知的图生文提供商: " + providerName);
        }
        this.activeImage2TextProvider = providerName;
        log.info("图生文提供商已切换为: {}", providerName);
    }

    public List<ProviderInfo> getText2ImageProviderList() {
        List<ProviderInfo> list = new ArrayList<>();
        for (Text2ImageProvider p : text2ImageProviders) {
            ProviderInfo info = new ProviderInfo();
            info.setName(p.getProviderName());
            info.setAvailable(p.isAvailable());
            info.setActive(p.getProviderName().equals(activeText2ImageProvider));
            list.add(info);
        }
        return list;
    }

    public List<ProviderInfo> getImage2TextProviderList() {
        List<ProviderInfo> list = new ArrayList<>();
        for (Image2TextProvider p : image2TextProviders) {
            ProviderInfo info = new ProviderInfo();
            info.setName(p.getProviderName());
            info.setAvailable(p.isAvailable());
            info.setActive(p.getProviderName().equals(activeImage2TextProvider));
            list.add(info);
        }
        return list;
    }

    public String getActiveText2ImageProviderName() {
        return activeText2ImageProvider;
    }

    public String getActiveImage2TextProviderName() {
        return activeImage2TextProvider;
    }

    private Text2ImageProvider findText2ImageProvider(String name) {
        return text2ImageProviders.stream()
                .filter(p -> p.getProviderName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Image2TextProvider findImage2TextProvider(String name) {
        return image2TextProviders.stream()
                .filter(p -> p.getProviderName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @lombok.Data
    public static class ProviderInfo {
        private String name;
        private boolean available;
        private boolean active;
    }
}
