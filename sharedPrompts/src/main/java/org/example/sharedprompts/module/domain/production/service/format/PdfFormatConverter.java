package org.example.sharedprompts.module.domain.production.service.format;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.format.markdown.MarkdownToHtmlConverter;
import org.example.sharedprompts.module.domain.production.service.format.pdf.HtmlToPdfConverter;
import org.example.sharedprompts.module.domain.production.service.format.pdf.PdfCssProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdfFormatConverter implements FormatConverter {

    private final MarkdownToHtmlConverter markdownToHtmlConverter;
    private final HtmlToPdfConverter htmlToPdfConverter;
    private final PdfCssProvider cssProvider;

    @Override
    public byte[] convert(String content, String fileName) {
        try {
            log.debug("Converting content to PDF - fileName: {}, contentLength: {}", fileName, content.length());

            String html = convertToHtml(content);
            String css = cssProvider.getDefaultCss();
            byte[] pdfBytes = htmlToPdfConverter.convert(html, css);

            log.debug("PDF conversion completed - fileName: {}, pdfSize: {} bytes", fileName, pdfBytes.length);
            return pdfBytes;

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

        boolean hasMarkdownPattern = content.contains("#") 
                || content.contains("*") 
                || content.contains("`") 
                || content.contains("[") 
                || content.contains("|")
                || content.contains("-") 
                || content.contains(">");

        if (!hasMarkdownPattern) {
            return false;
        }

        try {
            markdownToHtmlConverter.convert(content);
            return true;
        } catch (Exception e) {
            log.debug("Markdown parsing failed, treating as plain text: {}", e.getMessage());
            return false;
        }
    }

    private String wrapPlainTextInHtml(String text) {
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        
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
