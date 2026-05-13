package com.nebula.studio.ai.provider;

import com.nebula.studio.dto.request.Text2ImageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

@Slf4j
@Component
public class MockText2ImageProvider implements Text2ImageProvider {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Text2ImageResult generate(Text2ImageRequest request, String builtPrompt) {
        try {
            int width = request.getWidth() != null ? request.getWidth() : 512;
            int height = request.getHeight() != null ? request.getHeight() : 512;
            byte[] pngData = generatePlaceholderPng(width, height, builtPrompt);
            return Text2ImageResult.success(pngData, getProviderName());
        } catch (Exception e) {
            return Text2ImageResult.fail("模拟图片生成失败: " + e.getMessage(), getProviderName());
        }
    }

    private byte[] generatePlaceholderPng(int width, int height, String text) {
        try {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            Random random = new Random();
            float hue = random.nextFloat();
            g.setColor(Color.getHSBColor(hue, 0.3f, 0.2f));
            g.fillRect(0, 0, width, height);

            for (int i = 0; i < 30; i++) {
                float h2 = (hue + random.nextFloat() * 0.3f) % 1.0f;
                g.setColor(Color.getHSBColor(h2, 0.5f, 0.4f + random.nextFloat() * 0.3f));
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                int s = 20 + random.nextInt(80);
                g.fillOval(x, y, s, s);
            }

            g.setColor(Color.WHITE);
            g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
            FontMetrics fm = g.getFontMetrics();
            String displayText = text.length() > 30 ? text.substring(0, 30) + "..." : text;
            int textX = (width - fm.stringWidth(displayText)) / 2;
            int textY = height / 2;
            g.drawString(displayText, textX, textY);

            g.setColor(new Color(255, 255, 255, 128));
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString("[Mock Image]", 10, height - 20);

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
