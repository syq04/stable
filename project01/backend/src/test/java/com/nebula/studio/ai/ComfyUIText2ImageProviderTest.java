package com.nebula.studio.ai;

import com.nebula.studio.dto.request.Text2ImageRequest;
import com.nebula.studio.service.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ComfyUIText2ImageProviderTest {

    private DynamicConfigService mockConfigService;
    private ComfyUIClient client;
    private com.nebula.studio.ai.provider.ComfyUIText2ImageProvider provider;
    private Text2ImageRequest request;

    @BeforeEach
    void setUp() {
        mockConfigService = mock(DynamicConfigService.class);
        when(mockConfigService.getConfigValue(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        client = new ComfyUIClient("http://127.0.0.1:8188", 120, 2000,
                "workflow/comfyui_default.json", mockConfigService);

        provider = new com.nebula.studio.ai.provider.ComfyUIText2ImageProvider(client, mockConfigService);

        request = new Text2ImageRequest();
        request.setPrompt("a cute cat");
        request.setNegativePrompt("ugly");
        request.setWidth(512);
        request.setHeight(512);
        request.setSteps(20);
        request.setCfgScale(8.0);
        request.setSeed(12345L);
        request.setSamplerName("euler");
    }

    @Test
    void getProviderName_shouldReturnComfyui() {
        assertEquals("comfyui", provider.getProviderName());
    }

    @Test
    void isAvailable_shouldReturnTrueWhenEnabledAndServerRunning() {
        assertTrue(client.isAvailable(), "ComfyUI 应可连接 (http://127.0.0.1:8188)");
    }

    @Test
    void generate_shouldReturnSuccessResult() {
        var result = provider.generate(request, "a cute cat");
        assertTrue(result.isSuccess());
        assertNotNull(result.getImageData());
        assertTrue(result.getImageData().length > 0);
        assertEquals("comfyui", result.getProviderName());
    }

    @Test
    void generate_shouldReturnImageWithValidSize() {
        var result = provider.generate(request, "a cute cat");
        assertTrue(result.isSuccess());
        assertTrue(result.getImageData().length > 100,
                "生成的图片应大于 100 字节，实际: " + result.getImageData().length);
    }

    @Test
    void generate_shouldWorkWithoutSeed() {
        request.setSeed(null);
        var result = provider.generate(request, "a cute cat");
        assertTrue(result.isSuccess());
    }

    @Test
    void generate_shouldWorkWithoutNegativePrompt() {
        request.setNegativePrompt(null);
        var result = provider.generate(request, "a cute cat");
        assertTrue(result.isSuccess());
    }

    @Test
    void generate_shouldAcceptWideImage() {
        request.setWidth(768);
        request.setHeight(432);
        request.setSteps(15);
        var result = provider.generate(request, "a landscape view");
        assertTrue(result.isSuccess());
    }
}
