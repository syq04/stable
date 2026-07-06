package com.nebula.studio.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.studio.service.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class QwenImage2TextProvider implements Image2TextProvider {

    private final DynamicConfigService configService;
    private final ObjectMapper objectMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public QwenImage2TextProvider(DynamicConfigService configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "qwen";
    }

    @Override
    public boolean isAvailable() {
        String apiKey = configService.getConfigValue("ai.qwen.api-key");
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Image2TextResult analyze(byte[] imageBytes, String mimeType, String analysisType) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + (mimeType != null ? mimeType : "image/png") + ";base64," + base64;
        return analyzeInternal(dataUrl, analysisType);
    }

    @Override
    public Image2TextResult analyzeUrl(String imageUrl, String analysisType) {
        return analyzeInternal(imageUrl, analysisType);
    }

    private Image2TextResult analyzeInternal(String imageSource, String analysisType) {
        try {
            String apiKey = configService.getConfigValue("ai.qwen.api-key");
            if (apiKey == null || apiKey.isBlank()) {
                return Image2TextResult.fail("千问 API Key 未配置，请在系统配置中设置", getProviderName());
            }

            String apiUrl = configService.getConfigValue("ai.qwen.api-url",
                    "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
            String model = configService.getConfigValue("ai.qwen.model", "qwen3.5-omni-plus");

            String systemPrompt = buildSystemPrompt(analysisType);

            Map<String, Object> imageContent = new java.util.LinkedHashMap<>();
            imageContent.put("type", "image_url");
            imageContent.put("image_url", Map.of("url", imageSource));

            Map<String, Object> textContent = new java.util.LinkedHashMap<>();
            textContent.put("type", "text");
            textContent.put("text", "请分析这张图片");

            List<Map<String, Object>> userContent = List.of(imageContent, textContent);

            Map<String, Object> userMessage = Map.of("role", "user", "content", userContent);
            Map<String, Object> systemMessage = Map.of("role", "system", "content", systemPrompt);

            Map<String, Object> requestBody = new java.util.LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(systemMessage, userMessage));
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.7);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.info("Calling Qwen API: {}, model: {}, analysisType: {}", apiUrl, model, analysisType);

            Request httpRequest = new Request.Builder()
                    .url(apiUrl)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    log.error("Qwen API error HTTP {}: {}", response.code(), errBody);
                    return Image2TextResult.fail("千问 API 返回错误: HTTP " + response.code(), getProviderName());
                }

                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    return Image2TextResult.fail("千问 API 返回空响应", getProviderName());
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    return Image2TextResult.fail("千问 API 返回数据格式异常", getProviderName());
                }

                String content = choices.get(0).path("message").path("content").asText();
                if (content == null || content.isBlank()) {
                    return Image2TextResult.fail("千问 API 返回内容为空", getProviderName());
                }

                content = stripMarkdownCodeFence(content);
                return Image2TextResult.success(content, getProviderName());
            }
        } catch (IOException e) {
            log.error("Qwen API call failed: {}", e.getMessage());
            return Image2TextResult.fail("千问 API 调用失败: " + e.getMessage(), getProviderName());
        }
    }

    private String stripMarkdownCodeFence(String content) {
        if (content == null) return null;
        String trimmed = content.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private String extractJsonObject(String content) {
        if (content == null) return null;
        String s = content.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end < 0 || start > end) return null;
        return s.substring(start, end + 1);
    }

    @Override
    public Image2TextResult evaluateImageQuality(byte[] imageBytes, String mimeType, String originalPrompt) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + (mimeType != null ? mimeType : "image/png") + ";base64," + base64;

        try {
            String apiKey = configService.getConfigValue("ai.qwen.api-key");
            if (apiKey == null || apiKey.isBlank()) {
                return Image2TextResult.fail("千问 API Key 未配置，请在系统配置中设置", getProviderName());
            }

            String apiUrl = configService.getConfigValue("ai.qwen.api-url",
                    "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
            String model = configService.getConfigValue("ai.qwen.model", "qwen3.5-omni-plus");

            String evaluationPrompt = """
                    你是SD文生图质量评估助手。请仔细分析这张由Stable Diffusion根据提示词生成的图片。

                    原始提示词：%s

                    请严格按以下JSON格式返回评估结果，不要包含任何其他文字：
                    {
                      "description": "图片的详细中文描述",
                      "tags": ["标签1", "标签2", "标签3"],
                      "style": "艺术风格的中文描述",
                      "prompt": "适合作为文生图提示词的英文描述",
                      "accuracy": 85,
                      "accuracyDetail": "评分依据：简要说明哪些元素符合提示词，哪些缺失或不准确"
                    }

                    accuracy为0-100的数字，表示图片内容与原始提示词的匹配程度。评分标准：
                    - 主体是否与提示词一致（占40%%）
                    - 场景/背景是否与提示词一致（占20%%）
                    - 风格/氛围是否与提示词一致（占20%%）
                    - 细节/色彩/光线是否与提示词一致（占20%%）""".formatted(originalPrompt);

            Map<String, Object> imageContent = new java.util.LinkedHashMap<>();
            imageContent.put("type", "image_url");
            imageContent.put("image_url", Map.of("url", dataUrl));

            Map<String, Object> textContent = new java.util.LinkedHashMap<>();
            textContent.put("type", "text");
            textContent.put("text", "请评估这张图片与原始提示词的匹配程度");

            List<Map<String, Object>> userContent = List.of(imageContent, textContent);

            Map<String, Object> userMessage = Map.of("role", "user", "content", userContent);
            Map<String, Object> systemMessage = Map.of("role", "system", "content", evaluationPrompt);

            Map<String, Object> requestBody = new java.util.LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(systemMessage, userMessage));
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.3);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.info("Calling Qwen evaluate API: {}, model: {}", apiUrl, model);

            Request httpRequest = new Request.Builder()
                    .url(apiUrl)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    log.error("Qwen evaluate API error HTTP {}: {}", response.code(), errBody);
                    return Image2TextResult.fail("千问评估 API 返回错误: HTTP " + response.code(), getProviderName());
                }

                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    return Image2TextResult.fail("千问评估 API 返回空响应", getProviderName());
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    return Image2TextResult.fail("千问评估 API 返回数据格式异常", getProviderName());
                }

                String content = choices.get(0).path("message").path("content").asText();
                if (content == null || content.isBlank()) {
                    return Image2TextResult.fail("千问评估 API 返回内容为空", getProviderName());
                }

                content = stripMarkdownCodeFence(content);
                log.info("Qwen evaluate raw content (first 300 chars): {}", content.substring(0, Math.min(300, content.length())));

                Image2TextResult result = Image2TextResult.success(content, getProviderName());
                try {
                    String jsonContent = extractJsonObject(content);
                    if (jsonContent != null) {
                        JsonNode evalNode = objectMapper.readTree(jsonContent);
                        boolean hasAcc = evalNode.has("accuracy");
                        if (hasAcc) {
                            result.setAccuracy(evalNode.get("accuracy").asDouble());
                        }
                        result.setAccuracyDetail(evalNode.has("accuracyDetail") ? evalNode.get("accuracyDetail").asText() : null);
                        result.setDescription(jsonContent);
                        log.info("Qwen evaluate parsed: hasAccuracy={}, accuracy={}, keys={}", hasAcc, result.getAccuracy(), evalNode.fieldNames());
                    } else {
                        log.warn("Qwen evaluate: could not extract JSON object from content");
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse accuracy from evaluate response: {}", e.getMessage());
                }

                return result;
            }
        } catch (IOException e) {
            log.error("Qwen evaluate API call failed: {}", e.getMessage());
            return Image2TextResult.fail("千问评估 API 调用失败: " + e.getMessage(), getProviderName());
        }
    }

    private String buildSystemPrompt(String analysisType) {
        String type = analysisType != null ? analysisType : "general";

        String typeInstruction = switch (type) {
            case "artistic" ->
                "请重点分析这张图片的艺术风格，包括绘画手法、色彩运用、光影处理、构图方式、艺术流派等方面。";
            case "tags" ->
                "请为这张图片生成详细的中文标签列表，包含主体对象、场景、色彩、风格等维度。";
            default ->
                "请详细描述这张图片的内容，包括主体对象、场景、色彩、构图、氛围等方面。";
        };

        return """
                你是一个专业的图像分析助手。%s
                
                请严格按以下JSON格式返回分析结果，不要包含任何其他文字：
                {
                  "description": "图片的详细中文描述",
                  "tags": ["标签1", "标签2", "标签3"],
                  "style": "艺术风格的中文描述",
                  "prompt": "适合作为文生图提示词的英文描述，包含主体、场景、风格、光线等关键要素"
                }""".formatted(typeInstruction);
    }
}
