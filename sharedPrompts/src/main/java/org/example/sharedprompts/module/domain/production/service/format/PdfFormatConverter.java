package org.example.sharedprompts.module.domain.production.service.format;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
@Slf4j
public class PdfFormatConverter implements FormatConverter {

    @Override
    public byte[] convert(String content, String fileName) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            Font font = new Font(Font.HELVETICA, 11);
            String[] paragraphs = content.split("\n\n");
            for (String para : paragraphs) {
                document.add(new Paragraph(para.trim(), font));
                document.add(new Paragraph(" "));
            }

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("PDF conversion failed", e);
            throw new FormatConversionException("PDF conversion failed: " + e.getMessage(), e);
        }
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
