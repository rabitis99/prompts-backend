package org.example.sharedprompts.module.domain.production.service.image;

import java.io.IOException;

public interface ImageProcessor {

    byte[] resize(byte[] imageBytes, int width, int height) throws IOException;

    byte[] generateThumbnail(byte[] imageBytes, int size) throws IOException;

    ImageMetadata extractMetadata(byte[] imageBytes) throws IOException;
}
