package vn.hoadon.ai.knowledge.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import vn.hoadon.ai.knowledge.config.KnowledgeProperties;
import vn.hoadon.ai.knowledge.model.ExtractedKnowledge;
import vn.hoadon.ai.knowledge.model.KnowledgeDocumentType;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeContentExtractor {
    private static final int MAX_SHEETS = 100;
    private static final int MAX_ROWS = 50_000;
    private static final int MAX_CELLS = 250_000;

    private final KnowledgeProperties properties;

    public KnowledgeContentExtractor(KnowledgeProperties properties) {
        this.properties = properties;
        ZipSecureFile.setMinInflateRatio(0.01d);
        ZipSecureFile.setMaxEntrySize(Math.max(properties.getMaxUploadBytes() * 4, 20_000_000));
        ZipSecureFile.setMaxTextSize(properties.getMaxExtractedCharacters());
    }

    public ExtractedKnowledge extract(KnowledgeDocumentType type, byte[] source) {
        try {
            String content = switch (type) {
                case MD, TXT -> text(source);
                case PDF -> pdf(source);
                case DOCX -> docx(source);
                case XLSX -> xlsx(source);
                case XML, XSD -> xml(source);
            };
            boolean requiresOcr = type == KnowledgeDocumentType.PDF && content.replaceAll("\\s+", "").length() < 20;
            return new ExtractedKnowledge(limit(content), requiresOcr);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Không thể trích xuất nội dung tài liệu", ex);
        }
    }

    private String text(byte[] source) {
        String value = new String(source, StandardCharsets.UTF_8);
        if (value.startsWith("\uFEFF")) value = value.substring(1);
        return value.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private String pdf(byte[] source) throws IOException {
        try (PDDocument document = PDDocument.load(source)) {
            if (document.isEncrypted()) throw new IllegalArgumentException("Không hỗ trợ PDF được mã hóa");
            if (document.getNumberOfPages() > properties.getMaxPdfPages()) {
                throw new IllegalArgumentException("PDF vượt quá giới hạn số trang");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder result = new StringBuilder();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String value = stripper.getText(document).trim();
                if (!value.isEmpty()) result.append("\n\n## Trang ").append(page).append("\n\n").append(value);
                ensureWithinLimit(result.length());
            }
            return result.toString().trim();
        }
    }

    private String docx(byte[] source) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(source))) {
            StringBuilder result = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String value = paragraph.getText().trim();
                    if (value.isEmpty()) continue;
                    String style = paragraph.getStyle();
                    if (style != null && style.toLowerCase().startsWith("heading")) {
                        int level = parseHeadingLevel(style);
                        result.append("\n\n").append("#".repeat(level)).append(' ').append(value);
                    } else {
                        result.append("\n\n").append(value);
                    }
                } else if (element instanceof XWPFTable table) {
                    appendWordTable(result, table);
                }
                ensureWithinLimit(result.length());
            }
            return result.toString().trim();
        }
    }

    private void appendWordTable(StringBuilder result, XWPFTable table) {
        List<List<String>> rows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            rows.add(row.getTableCells().stream().map(cell -> cleanCell(cell.getText())).toList());
        }
        appendMarkdownTable(result, rows);
    }

    private String xlsx(byte[] source) throws IOException {
        StringBuilder result = new StringBuilder();
        DataFormatter formatter = new DataFormatter();
        int rowCount = 0;
        int cellCount = 0;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(source))) {
            if (workbook.getNumberOfSheets() > MAX_SHEETS) throw new IllegalArgumentException("File Excel có quá nhiều sheet");
            for (Sheet sheet : workbook) {
                result.append("\n\n## Sheet: ").append(sheet.getSheetName()).append("\n");
                List<List<String>> rows = new ArrayList<>();
                for (Row row : sheet) {
                    if (++rowCount > MAX_ROWS) throw new IllegalArgumentException("File Excel có quá nhiều dòng");
                    int width = Math.min(Math.max(1, row.getLastCellNum()), 200);
                    List<String> values = new ArrayList<>(width);
                    for (int index = 0; index < width; index++) {
                        if (++cellCount > MAX_CELLS) throw new IllegalArgumentException("File Excel có quá nhiều ô dữ liệu");
                        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        values.add(cell == null ? "" : cleanCell(formatter.formatCellValue(cell)));
                    }
                    rows.add(values);
                }
                appendMarkdownTable(result, rows);
                ensureWithinLimit(result.length());
            }
        }
        return result.toString().trim();
    }

    private String xml(byte[] source) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        org.w3c.dom.Document document;
        try {
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            document = builder.parse(new ByteArrayInputStream(source));
        } catch (SAXException ex) {
            throw new IllegalArgumentException("XML/XSD không hợp lệ hoặc chứa khai báo không an toàn", ex);
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return "```xml\n" + writer + "\n```";
    }

    private void appendMarkdownTable(StringBuilder result, List<List<String>> rows) {
        if (rows.isEmpty()) return;
        int width = rows.stream().mapToInt(List::size).max().orElse(1);
        result.append("\n\n");
        appendRow(result, rows.get(0), width);
        result.append('|');
        for (int index = 0; index < width; index++) result.append(" --- |");
        result.append('\n');
        for (int index = 1; index < rows.size(); index++) appendRow(result, rows.get(index), width);
    }

    private void appendRow(StringBuilder result, List<String> row, int width) {
        result.append('|');
        for (int index = 0; index < width; index++) {
            String value = index < row.size() ? row.get(index) : "";
            result.append(' ').append(value).append(" |");
        }
        result.append('\n');
    }

    private int parseHeadingLevel(String style) {
        String digits = style.replaceAll("\\D+", "");
        if (digits.isEmpty()) return 2;
        return Math.max(1, Math.min(6, Integer.parseInt(digits)));
    }

    private String cleanCell(String value) {
        return value == null ? "" : value.replace("|", "\\|").replaceAll("[\\r\\n]+", " ").trim();
    }

    private String limit(String value) {
        String content = value == null ? "" : value.trim();
        ensureWithinLimit(content.length());
        return content;
    }

    private void ensureWithinLimit(int length) {
        if (length > properties.getMaxExtractedCharacters()) {
            throw new IllegalArgumentException("Nội dung trích xuất vượt quá giới hạn xử lý");
        }
    }
}
