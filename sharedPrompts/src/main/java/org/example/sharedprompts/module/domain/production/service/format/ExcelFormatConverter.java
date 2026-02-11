package org.example.sharedprompts.module.domain.production.service.format;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class ExcelFormatConverter implements FormatConverter {

    @Override
    public byte[] convert(String content, String fileName) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Content");

            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                XSSFRow row = sheet.createRow(i);
                String line = lines[i];

                // CSV-like 라인이면 셀 분리, 아니면 단일 셀
                String[] cells = line.contains(",") ? line.split(",") : new String[]{line};
                for (int j = 0; j < cells.length; j++) {
                    XSSFCell cell = row.createCell(j);
                    cell.setCellValue(cells[j].trim());
                }
            }

            // 첫 번째 열 자동 너비 조정
            if (lines.length > 0) {
                sheet.autoSizeColumn(0);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Excel conversion failed", e);
            throw new FormatConversionException("Excel conversion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String getFileExtension() {
        return ".xlsx";
    }

    @Override
    public boolean supports(String format) {
        return "xlsx".equals(format) || "excel".equals(format);
    }
}
