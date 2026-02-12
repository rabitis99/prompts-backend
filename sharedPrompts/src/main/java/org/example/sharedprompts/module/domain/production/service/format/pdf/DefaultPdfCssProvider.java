package org.example.sharedprompts.module.domain.production.service.format.pdf;

import org.springframework.stereotype.Component;

@Component
public class DefaultPdfCssProvider implements PdfCssProvider {

    @Override
    public String getDefaultCss() {
        return """
                @page {
                    size: A4;
                    margin: 2cm;
                }
                body {
                    font-family: sans-serif;
                    font-size: 11pt;
                    line-height: 1.6;
                    color: #333;
                }
                h1 { font-size: 22pt; border-bottom: 2pt solid #333; padding-bottom: 6pt; margin-top: 20pt; }
                h2 { font-size: 17pt; margin-top: 16pt; }
                h3 { font-size: 14pt; margin-top: 12pt; }
                p { margin: 8pt 0; }
                code {
                    background-color: #f5f5f5;
                    padding: 2pt 4pt;
                    font-family: monospace;
                    font-size: 10pt;
                }
                pre {
                    background-color: #f5f5f5;
                    padding: 10pt;
                    border-left: 4pt solid #007acc;
                    page-break-inside: avoid;
                }
                pre code { background-color: transparent; padding: 0; }
                blockquote {
                    border-left: 4pt solid #ddd;
                    padding-left: 12pt;
                    color: #666;
                    font-style: italic;
                }
                table { width: 100%; border-collapse: collapse; margin: 12pt 0; }
                th, td { border: 1pt solid #ddd; padding: 8pt; text-align: left; }
                th { background-color: #f0f0f0; font-weight: bold; }
                img { max-width: 100%; height: auto; }
                """;
    }
}

