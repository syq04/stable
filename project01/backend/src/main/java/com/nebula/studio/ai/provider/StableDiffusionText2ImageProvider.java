package com.nebula.studio.ai.provider;

import com.nebula.studio.ai.SdImageResponse;
import com.nebula.studio.ai.SdTxt2ImgRequest;
import com.nebula.studio.ai.StableDiffusionClient;
import com.nebula.studio.dto.request.Text2ImageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class StableDiffusionText2ImageProvider implements Text2ImageProvider {

    private final StableDiffusionClient sdClient;

    @Value("${ai.sd.enabled:false}")
    private boolean sdEnabled;

    @Override
    public String getProviderName() {
        return "stable-diffusion";
    }

    @Override
    public boolean isAvailable() {
        return sdEnabled && sdClient.isAvailable();
    }

    @Override
    public Text2ImageResult generate(Text2ImageRequest request, String builtPrompt) {
        try {
            SdTxt2ImgRequest sdReq = new SdTxt2ImgRequest();
            sdReq.setPrompt(builtPrompt);
            if (StringUtils.hasText(request.getNegativePrompt())) {
                sdReq.setNegativePrompt(request.getNegativePrompt());
            }
            sdReq.setWidth(request.getWidth() != null ? request.getWidth() : 512);
            sdReq.setHeight(request.getHeight() != null ? request.getHeight() : 512);
            sdReq.setSteps(request.getSteps() != null ? request.getSteps() : 20);
            sdReq.setCfgScale(request.getCfgScale() != null ? request.getCfgScale() : 7.5);
            if (request.getSeed() != null) {
                sdReq.setSeed(request.getSeed());
            }
            if (StringUtils.hasText(request.getSamplerName())) {
                sdReq.setSamplerName(request.getSamplerName());
            }

            SdImageResponse sdResp = sdClient.txt2img(sdReq);

            if (sdResp.getBase64Image() == null || sdResp.getBase64Image().isBlank()) {
                return Text2ImageResult.fail("SD API 未返回图片数据", getProviderName());
            }

            byte[] imageData = Base64.getDecoder().decode(sdResp.getBase64Image());
            Text2ImageResult result = Text2ImageResult.success(imageData, getProviderName());
            result.setSeed(sdResp.getSeed());
            return result;
        } catch (Exception e) {
            log.error("Stable Diffusion 调用失败: {}", e.getMessage());
            return Text2ImageResult.fail("调用失败: " + e.getMessage(), getProviderName());
        }
    }
}
