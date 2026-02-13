package org.example.sharedprompts.module.domain.production.service.image;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class JavaImageProcessor implements ImageProcessor {

    @Override
    public byte[] resize(byte[] imageBytes, int width, int height) throws IOException {
        if (width <= 0 || height <= 0) {
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Resize dimensions must be positive: " + width + "x" + height);
        }
        BufferedImage original = readImage(imageBytes);

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        try {
            applyHighQualityRendering(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(original, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }

        return encodeToJpeg(resized);
    }

    @Override
    public byte[] generateThumbnail(byte[] imageBytes, int size) throws IOException {
        if (size <= 0) {
            throw new BaseException(
                    ModuleErrorCode.VALIDATION_ERROR,
                    null,
                    "Thumbnail size must be positive: " + size);
        }
        BufferedImage original = readImage(imageBytes);

        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        if (originalWidth <= 0 || originalHeight <= 0) {
            throw new IOException("Invalid image dimensions: " + originalWidth + "x" + originalHeight);
        }

        int thumbWidth, thumbHeight;
        if (originalWidth > originalHeight) {
            thumbWidth = size;
            thumbHeight = Math.max(1, (int) ((double) originalHeight / originalWidth * size));
        } else {
            thumbHeight = size;
            thumbWidth = Math.max(1, (int) ((double) originalWidth / originalHeight * size));
        }

        BufferedImage thumbnail = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size, size);
            applyHighQualityRendering(g);

            int x = (size - thumbWidth) / 2;
            int y = (size - thumbHeight) / 2;
            g.drawImage(original, x, y, thumbWidth, thumbHeight, null);
        } finally {
            g.dispose();
        }

        return encodeToJpeg(thumbnail);
    }

    @Override
    public ImageMetadata extractMetadata(byte[] imageBytes) throws IOException {
        BufferedImage image = readImage(imageBytes);
        String detectedFormat = detectFormat(imageBytes);
        String mimeType = "jpg".equals(detectedFormat) ? "image/jpeg" : "image/" + detectedFormat;
        String format = detectedFormat.toUpperCase();

        return new ImageMetadata(
                image.getWidth(),
                image.getHeight(),
                imageBytes.length,
                format,
                mimeType
        );
    }

    private BufferedImage readImage(byte[] imageBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Failed to read image: unsupported format or corrupted data");
        }
        return image;
    }

    private void applyHighQualityRendering(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private byte[] encodeToJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "jpg", out)) {
            throw new IOException("Failed to encode image as JPEG");
        }
        return out.toByteArray();
    }

    private String detectFormat(byte[] imageBytes) {
        if (imageBytes.length >= 4) {
            if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == 0x50) {
                return "png";
            }
            if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8) {
                return "jpg";
            }
            if (imageBytes[0] == 0x47 && imageBytes[1] == 0x49 && imageBytes[2] == 0x46) {
                return "gif";
            }
            if (imageBytes.length >= 12
                    && imageBytes[0] == 0x52 && imageBytes[1] == 0x49 && imageBytes[2] == 0x46 && imageBytes[3] == 0x46
                    && imageBytes[8] == 0x57 && imageBytes[9] == 0x45 && imageBytes[10] == 0x42 && imageBytes[11] == 0x50) {
                return "webp";
            }
        }
        return "jpg";
    }
}
