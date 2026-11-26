package vn.hoadon.ai.document.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.ai.document.controller.DocumentRecognitionDtos;
import vn.hoadon.ai.document.service.DocumentFieldExtractor.ParsedDocument;
import vn.hoadon.ai.document.service.DocumentFieldExtractor.ParsedField;
import vn.hoadon.ai.document.service.DocumentFieldExtractor.ParsedItem;
import vn.hoadon.ai.ocr.config.OcrProperties;
import vn.hoadon.ai.ocr.model.OcrDocument;
import vn.hoadon.ai.ocr.service.LocalOcrService;
import vn.hoadon.entity.CustomersEntity;
import vn.hoadon.entity.ProductsEntity;
import vn.hoadon.repositories.CustomerRepository;
import vn.hoadon.repositories.ProductRepository;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DocumentRecognitionService {
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");

    private final LocalOcrService ocrService;
    private final DocumentFieldExtractor fieldExtractor;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OcrProperties properties;

    public DocumentRecognitionService(LocalOcrService ocrService, DocumentFieldExtractor fieldExtractor,
                                      CustomerRepository customerRepository, ProductRepository productRepository,
                                      OcrProperties properties) {
        this.ocrService = ocrService;
        this.fieldExtractor = fieldExtractor;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.properties = properties;
    }

    public DocumentRecognitionDtos.RecognitionResponse recognize(long companyId, int category, MultipartFile file) {
        if (!properties.isEnabled()) {
            throw new DocumentRecognitionException("document_recognition_disabled",
                    "Chức năng nhận diện chứng từ đang tắt");
        }
        if (category != 1 && category != 2 && category != 6) {
            throw new IllegalArgumentException("Chỉ hỗ trợ nhận diện cho hóa đơn và phiếu xuất kho");
        }
        ValidatedUpload upload = validate(file);
        OcrDocument document;
        try {
            document = ocrService.extract(upload.bytes(), upload.filename());
        } catch (IllegalStateException ex) {
            throw new DocumentRecognitionException("ocr_unavailable", ex.getMessage(), ex);
        }
        if (!document.complete()) {
            throw new DocumentRecognitionException("ocr_incomplete",
                    "Chứng từ có trang scan nhưng Tesseract chưa sẵn sàng hoặc không nhận diện được");
        }
        ParsedDocument parsed = fieldExtractor.extract(document.text(), document.averageConfidence());
        Page<CustomersEntity> customerPage = customerRepository.findAll((root, query, builder) -> builder.and(
                        builder.equal(root.get("companyId"), companyId), builder.equal(root.get("status"), 1)),
                PageRequest.of(0, 500));
        CustomerMatch customer = matchCustomer(parsed.buyerTaxCode(), parsed.buyerName(), customerPage);
        List<String> warnings = new ArrayList<>();
        if (customer == null && (!parsed.buyerTaxCode().empty() || !parsed.buyerName().empty())) {
            warnings.add("Không tìm thấy khách hàng khớp chính xác trong danh mục nội bộ");
        }
        Page<ProductsEntity> productPage = parsed.items().isEmpty() ? Page.empty()
                : productRepository.findAll((root, query, builder) -> builder.and(
                                builder.equal(root.get("companyId"), companyId), builder.equal(root.get("status"), 1)),
                        PageRequest.of(0, 500));
        List<DocumentRecognitionDtos.ItemView> items = new ArrayList<>();
        for (ParsedItem item : parsed.items()) {
            ProductMatch product = matchProduct(item.name(), productPage);
            if (product == null) warnings.add("Chưa đối chiếu được hàng hóa: " + item.name().value());
            ParsedField vatRate = category == 2 || category == 6
                    ? new ParsedField("-1", 1d, "Chứng từ không áp dụng thuế GTGT")
                    : item.vatRate();
            items.add(new DocumentRecognitionDtos.ItemView(fieldExtractor.view(item.name()),
                    fieldExtractor.view(product != null && item.unit().empty()
                            ? new ParsedField(product.entity().getUnit(), 1d, "Danh mục hàng hóa nội bộ") : item.unit()),
                    fieldExtractor.view(item.quantity()), fieldExtractor.view(item.unitPrice()),
                    fieldExtractor.view(vatRate), fieldExtractor.view(item.lineTotal()),
                    product == null ? null : product.entity().getId(),
                    product == null ? null : product.entity().getCode()));
        }
        addMissingWarnings(parsed, items, warnings);
        String sourceType = sourceType(upload.extension(), document);
        return new DocumentRecognitionDtos.RecognitionResponse(upload.filename(), sourceType, document.pageCount(),
                document.ocrUsed(), round(document.averageConfidence()), document.durationMs(),
                party(parsed.sellerName(), parsed.sellerTaxCode(), parsed.sellerAddress(), null),
                party(parsed.buyerName(), parsed.buyerTaxCode(), parsed.buyerAddress(), customer),
                fieldExtractor.view(parsed.documentDate()), fieldExtractor.view(parsed.currency()), items,
                fieldExtractor.view(parsed.subtotal()), fieldExtractor.view(parsed.vatAmount()),
                fieldExtractor.view(parsed.total()), warnings.stream().distinct().toList());
    }

    private DocumentRecognitionDtos.PartyView party(ParsedField name, ParsedField taxCode, ParsedField address,
                                                      CustomerMatch match) {
        return new DocumentRecognitionDtos.PartyView(fieldExtractor.view(name), fieldExtractor.view(taxCode),
                fieldExtractor.view(address), match == null ? null : match.entity().getId(),
                match == null ? null : match.entity().getCode());
    }

    private CustomerMatch matchCustomer(ParsedField taxCode, ParsedField name, Page<CustomersEntity> page) {
        if (page.hasNext()) return null;
        String normalizedTax = digits(taxCode.value());
        List<CustomersEntity> candidates = page.getContent();
        if (!normalizedTax.isBlank()) {
            List<CustomersEntity> exact = candidates.stream()
                    .filter(value -> normalizedTax.equals(digits(value.getTaxCode()))).toList();
            if (exact.size() == 1) return new CustomerMatch(exact.get(0));
        }
        String normalizedName = normalize(name.value());
        if (!normalizedName.isBlank()) {
            List<CustomersEntity> exact = candidates.stream().filter(value -> normalizedName.equals(
                    normalize(firstNonBlank(value.getCompanyName(), value.getBuyerName())))).toList();
            if (exact.size() == 1) return new CustomerMatch(exact.get(0));
        }
        return null;
    }

    private ProductMatch matchProduct(ParsedField name, Page<ProductsEntity> page) {
        if (page.hasNext()) return null;
        String normalized = normalize(name.value());
        if (normalized.isBlank()) return null;
        List<ProductsEntity> candidates = page.getContent();
        List<ProductsEntity> exact = candidates.stream().filter(value -> normalized.equals(normalize(value.getName()))
                || normalized.equals(normalize(value.getCode()))).toList();
        return exact.size() == 1 ? new ProductMatch(exact.get(0)) : null;
    }

    private void addMissingWarnings(ParsedDocument parsed, List<DocumentRecognitionDtos.ItemView> items,
                                    List<String> warnings) {
        if (parsed.buyerName().empty() && parsed.buyerTaxCode().empty()) {
            warnings.add("Chưa nhận diện được thông tin người mua");
        }
        if (parsed.documentDate().empty()) warnings.add("Chưa nhận diện được ngày chứng từ");
        if (items.isEmpty()) warnings.add("Chưa nhận diện chắc chắn được dòng hàng hóa, vui lòng nhập thủ công");
        if (parsed.total().empty()) warnings.add("Chưa nhận diện được tổng tiền thanh toán");
        if (documentLowConfidence(parsed, items)) {
            warnings.add("Một số trường có độ tin cậy thấp và cần được kiểm tra trước khi áp dụng");
        }
    }

    private boolean documentLowConfidence(ParsedDocument parsed, List<DocumentRecognitionDtos.ItemView> items) {
        double threshold = properties.getLowConfidenceThreshold();
        if ((!parsed.buyerName().empty() && parsed.buyerName().confidence() < threshold)
                || (!parsed.documentDate().empty() && parsed.documentDate().confidence() < threshold)
                || (!parsed.total().empty() && parsed.total().confidence() < threshold)) return true;
        return items.stream().anyMatch(item -> item.name().lowConfidence() || item.quantity().lowConfidence()
                || item.unitPrice().lowConfidence());
    }

    private ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ảnh hoặc PDF chứng từ");
        if (file.getSize() > properties.getMaxUploadBytes()) {
            throw new IllegalArgumentException("Chứng từ vượt quá giới hạn dung lượng");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.length() > 255 || filename.contains("/")
                || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("Tên file chứng từ không hợp lệ");
        }
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Chỉ hỗ trợ JPG, JPEG, PNG và PDF");
        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException ex) { throw new IllegalArgumentException("Không thể đọc chứng từ tải lên", ex); }
        if (bytes.length > properties.getMaxUploadBytes()) {
            throw new IllegalArgumentException("Chứng từ vượt quá giới hạn dung lượng");
        }
        return new ValidatedUpload(filename.trim(), extension, bytes);
    }

    private String sourceType(String extension, OcrDocument document) {
        if (!"pdf".equals(extension)) return "IMAGE_OCR";
        if (!document.ocrUsed()) return "PDF_TEXT";
        return document.pages().stream().anyMatch(page -> "PDF_TEXT".equals(page.method())) ? "PDF_MIXED" : "PDF_OCR";
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd').replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
    private double round(double value) { return Math.round(value * 10_000d) / 10_000d; }

    private record ValidatedUpload(String filename, String extension, byte[] bytes) { }
    private record CustomerMatch(CustomersEntity entity) { }
    private record ProductMatch(ProductsEntity entity) { }
}
