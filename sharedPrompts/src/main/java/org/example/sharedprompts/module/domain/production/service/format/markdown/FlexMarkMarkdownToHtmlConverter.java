package org.example.sharedprompts.module.domain.production.service.format.markdown;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.format.FormatConversionException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class FlexMarkMarkdownToHtmlConverter implements MarkdownToHtmlConverter {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public FlexMarkMarkdownToHtmlConverter() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create()
        ));
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public String convert(String markdown) {
        try {
            Node document = parser.parse(markdown);
            return renderer.render(document);
        } catch (Exception e) {
            log.error("Markdown to HTML conversion failed", e);
            throw new FormatConversionException("Markdown to HTML conversion failed: " + e.getMessage(), e);
        }
    }
}

