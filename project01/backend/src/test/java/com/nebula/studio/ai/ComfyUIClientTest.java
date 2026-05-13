package com.nebula.studio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.studio.service.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ComfyUIClientTest {

    private DynamicConfigService mockConfigService;
    private ComfyUIClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockConfigService = mock(DynamicConfigService.class);
        when(mockConfigService.getConfigValue(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        client = new ComfyUIClient("http://127.0.0.1:8188", 120, 2000,
                "workflow/comfyui_default.json", mockConfigService);
        objectMapper = new ObjectMapper();
    }

    @Test
    void loadWorkflow_shouldReturnValidWorkflow() throws Exception {
        JsonNode workflow = client.loadWorkflow();
        assertNotNull(workflow);
        assertTrue(workflow.has("19"), "应包含 VAE 解码节点");
        assertTrue(workflow.has("21"), "应包含 Checkpoint 加载节点");
        assertTrue(workflow.has("22"), "应包含负向提示词节点");
        assertTrue(workflow.has("24"), "应包含 KSampler 节点");
        assertTrue(workflow.has("25"), "应包含空 Latent 节点");
        assertTrue(workflow.has("26"), "应包含正向提示词节点");
    }

    @Test
    void loadWorkflow_shouldHaveCorrectNodeTypes() throws Exception {
        JsonNode workflow = client.loadWorkflow();
        assertEquals("CLIPTextEncode", workflow.get("22").get("class_type").asText());
        assertEquals("CLIPTextEncode", workflow.get("26").get("class_type").asText());
        assertEquals("KSampler", workflow.get("24").get("class_type").asText());
        assertEquals("EmptyLatentImage", workflow.get("25").get("class_type").asText());
    }

    @Test
    void loadWorkflow_shouldThrowWhenFileNotFound() {
        ComfyUIClient badClient = new ComfyUIClient("http://x", 120, 2000,
                "nonexistent.json", mockConfigService);
        assertThrows(IOException.class, badClient::loadWorkflow);
    }

    @Test
    void getEffectiveApiUrl_shouldReturnDefault() {
        when(mockConfigService.getConfigValue("ai.comfyui.api-url", "http://127.0.0.1:8188"))
                .thenReturn("http://127.0.0.1:8188");
        assertEquals("http://127.0.0.1:8188", client.getEffectiveApiUrl());
    }

    @Test
    void getEffectiveApiUrl_shouldReturnDbValueWhenSet() {
        when(mockConfigService.getConfigValue("ai.comfyui.api-url", "http://127.0.0.1:8188"))
                .thenReturn("http://10.0.0.99:8188");
        assertEquals("http://10.0.0.99:8188", client.getEffectiveApiUrl());
    }

    @Test
    void injectParams_shouldSetPositivePrompt() throws Exception {
        JsonNode workflow = client.loadWorkflow();
        client.injectParams(workflow, "a cute cat", "ugly", 512, 512, 20, 8.0, 12345L, "euler");
        assertEquals("a cute cat", workflow.get("26").get("inputs").get("text").asText());
    }

    @Test
    void injectParams_shouldSetNegativePrompt() throws Exception {
        JsonNode workflow = client.loadWorkflow();
        client.injectParams(workflow, "cat", "bad quality, blurry", 512, 512, 20, 8.0, null, null);
        assertEquals("bad quality, blurry", workflow.get("22").get("inputs").get("text").asText());
    }

    @Test
    void injectParams_shouldSetSamplerParams() throws Exception {
        JsonNode workflow = client.loadWorkflow();
        client.injectParams(workflow, "cat", null, 512, 512, 35, 9.5, 99999L, "dpmpp_2m");
        JsonNode inputs = workflow.get("24").get("inputs");
        assertEquals(99999L, inputs.get("seed").asLong());
        assertEquals(35, inputs.get("steps").asInt());
        assertEquals(9.5, inputs.get("cfg").asDouble(), 0.001);
        assertEquals("dpmpp_2m", inputs.get("sampler_name").asText());
    }

    @Test
    void injectParams_shouldNotOverrideSeedWhenNull() throws Exception {
        JsonNode workflow = client.loadWorkflow();
        long originalSeed = workflow.get("24").get("inputs").get("seed").asLong();
        client.injectParams(workflow, "cat", null, 512, 512, 20, 8.0, null, null);
        assertEquals(originalSeed, workflow.get("24").get("inputs").get("seed").asLong());
    }

    @Test
    void injectParams_shouldSetImageSize() throws Exception {
        JsonNode workflow = client.loadWorkflow();
        client.injectParams(workflow, "cat", null, 1024, 768, 20, 8.0, null, null);
        JsonNode inputs = workflow.get("25").get("inputs");
        assertEquals(1024, inputs.get("width").asInt());
        assertEquals(768, inputs.get("height").asInt());
    }

    @Test
    void injectParams_shouldSetSamplerNameOnlyWhenNonNull() throws Exception {
        JsonNode workflow = client.loadWorkflow();
        String original = workflow.get("24").get("inputs").get("sampler_name").asText();
        client.injectParams(workflow, "cat", null, 512, 512, 20, 8.0, null, null);
        assertEquals(original, workflow.get("24").get("inputs").get("sampler_name").asText());
    }

    @Test
    void downloadFirstImage_shouldExtractFilename() throws Exception {
        String historyJson = """
        {"outputs":{"19":{"images":[{"filename":"ComfyUI_00001_.png","type":"output","subfolder":""}]}}}
        """;
        JsonNode history = objectMapper.readTree(historyJson);
        assertNotNull(client.downloadFirstImage(history, "http://127.0.0.1:8188"));
    }

    @Test
    void downloadFirstImage_shouldReturnNullWhenNoOutputs() throws Exception {
        JsonNode history = objectMapper.readTree("{}");
        assertNull(client.downloadFirstImage(history, "http://127.0.0.1:8188"));
    }

    @Test
    void downloadFirstImage_shouldReturnNullWhenOutputsEmpty() throws Exception {
        JsonNode history = objectMapper.readTree("{\"outputs\":{}}");
        assertNull(client.downloadFirstImage(history, "http://127.0.0.1:8188"));
    }
}
