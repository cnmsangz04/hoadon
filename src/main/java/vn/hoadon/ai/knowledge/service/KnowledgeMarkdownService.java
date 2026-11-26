package vn.hoadon.ai.knowledge.service;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeMarkdownService {
    private final Parser parser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .maxOpenBlockParsers(100)
            .maxInlineNesting(100)
            .build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(List.of(TablesExtension.create()))
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    public String renderSafeHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        if (markdown.length() > 500_000) throw new IllegalArgumentException("Nội dung Markdown xem trước quá lớn");
        return renderer.render(parser.parse(markdown));
    }
}
