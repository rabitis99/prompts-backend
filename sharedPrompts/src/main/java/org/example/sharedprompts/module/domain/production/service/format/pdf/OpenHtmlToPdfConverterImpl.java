package org.example.sharedprompts.module.domain.production.service.format.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.format.FormatConversionException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
@Slf4j
public class OpenHtmlToPdfConverterImpl implements HtmlToPdfConverter {

    @Override
    public byte[] convert(String html, String css) {
        String fullHtml = wrapInHtmlDocument(html, css);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(fullHtml, null);
            builder.toStream(out);
            builder.run();

            return out.toByteArray();
        } catch (Exception e) {
            log.error("HTML to PDF conversion failed", e);
            throw new FormatConversionException("HTML to PDF conversion failed: " + e.getMessage(), e);
        }
    }

    private String wrapInHtmlDocument(String bodyHtml, String css) {
        String effectiveCss = (css != null && !css.isBlank()) ? css : "";
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8"/>
                    <style>
                    %s
                    </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(effectiveCss, bodyHtml);
    }
}

