package com.nebula.studio.ai.provider;

import com.nebula.studio.dto.request.Text2ImageRequest;

public interface Text2ImageProvider {

    String getProviderName();

    boolean isAvailable();

    Text2ImageResult generate(Text2ImageRequest request, String builtPrompt);
}
