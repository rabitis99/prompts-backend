package org.example.sharedprompts.module.domain.production.service.format;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.format.markdown.MarkdownToHtmlConverter;
import org.example.sharedprompts.module.domain.production.service.format.pdf.HtmlToPdfConverter;
import org.example.sharedprompts.module.domain.production.service.format.pdf.PdfCssProvider;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdfFormatConverter implements FormatConverter {

    private final MarkdownToHtmlConverter markdownToHtmlConverter;
    private final HtmlToPdfConverter htmlToPdfConverter;
    private final PdfCssProvider cssProvider;

    private static final Pattern MARKDOWN_PATTERN = Pattern.compile(
            "(?m)(^#{1,6}\\s|^[*\\-+]\\s|^\\d+\\.\\s|^```|\\[.+]\\(.+\\)|^>\\s|^\\|.+\\|)"
    );

    @Override
    public byte[] convert(String content, String fileName) {
        try {
            if (content == null) {
                throw new FormatConversionException("Content must not be null for file: " + fileName);
            }
            log.debug("Converting content to PDF - fileName: {}, contentLength: {}", fileName, content.length());

            String html = convertToHtml(content);
            String css = cssProvider.getDefaultCss();
            byte[] pdfBytes = htmlToPdfConverter.convert(html, css);

            log.debug("PDF conversion completed - fileName: {}, pdfSize: {} bytes", fileName, pdfBytes.length);
            return pdfBytes;

        } catch (FormatConversionException e) {
            log.error("PDF conversion failed - fileName: {}", fileName, e);
            throw e;
        } catch (Exception e) {
            log.error("PDF conversion failed - fileName: {}", fileName, e);
            throw new FormatConversionException("PDF conversion failed: " + e.getMessage(), e);
        }
    }

    private String convertToHtml(String content) {
        if (isMarkdown(content)) {
            log.debug("Detected Markdown content, converting to HTML");
            return markdownToHtmlConverter.convert(content);
        } else {
            log.debug("Plain text content detected, wrapping in HTML");
            return wrapPlainTextInHtml(content);
        }
    }

    private boolean isMarkdown(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return MARKDOWN_PATTERN.matcher(content).find();
    }

    private String wrapPlainTextInHtml(String text) {
        // HTML 이스케이프 처리 (UTF-8 문자는 그대로 유지)
        // 순서 중요: &를 먼저 처리해야 다른 이스케이프가 깨지지 않음
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\r\n", "<br>")
                .replace("\r", "<br>")
                .replace("\n", "<br>");
        
        // UTF-8 문자를 보존하기 위해 <p> 태그로 감싸서 반환
        // OpenHtmlToPdfConverterImpl에서 UTF-8 meta 태그가 추가됨
        return "<p>" + escaped + "</p>";
    }

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFileExtension() {
        return ".pdf";
    }

    @Override
    public boolean supports(String format) {
        return "pdf".equals(format);
    }
}
