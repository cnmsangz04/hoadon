package vn.hoadon.invoice.template.storage;

import vn.hoadon.invoice.template.render.InvoicePdfRenderer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.service.*;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class InvoiceAssetService {
    public record AssetRef(Long assetId, String sha256, String mimeType) { }
    private final InvoiceAssetRepository repository;
    private final InvoiceAssetPinRepository pins;
    private final DocumentStorage storage;
    private final InvoiceTemplateProperties properties;
    private final ObjectMapper mapper;

    public InvoiceAssetService(InvoiceAssetRepository repository, InvoiceAssetPinRepository pins, DocumentStorage storage,
                               InvoiceTemplateProperties properties, ObjectMapper mapper) {
        this.repository = repository;
        this.pins = pins;
        this.storage = storage;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Transactional
    public InvoiceAssetEntity upload(byte[] bytes, String kind, TemplateAccess access, boolean system) {
        if (system) access.requireSystemManager(); else access.requireCompany();
        if (bytes.length == 0 || bytes.length > properties.getMaxInputBytes()) throw TemplateException.invalid("ASSET_SIZE_LIMIT", "Kích thước tài nguyên không hợp lệ");
        String mime;
        Map<String, Object> metadata = new LinkedHashMap<>();
        try {
            switch (kind) {
                case "IMAGE" -> {
                    try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                        var readers = ImageIO.getImageReaders(input);
                        if (!readers.hasNext()) throw TemplateException.invalid("INVALID_IMAGE", "Ảnh không hợp lệ");
                        var reader = readers.next();
                        try {
                            reader.setInput(input);
                            int width = reader.getWidth(0), height = reader.getHeight(0);
                            if (width < 1 || height < 1 || (long) width * height > 20_000_000) throw TemplateException.invalid("IMAGE_SIZE_LIMIT", "Ảnh vượt giới hạn điểm ảnh");
                            String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                            if (!Set.of("png", "jpeg", "jpg").contains(format)) throw TemplateException.invalid("INVALID_IMAGE", "Chỉ hỗ trợ PNG và JPEG");
                            mime = format.equals("png") ? "image/png" : "image/jpeg";
                            metadata.put("width", width);
                            metadata.put("height", height);
                        } finally { reader.dispose(); }
                    }
                }
                case "FONT" -> {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bytes));
                    if (font.canDisplayUpTo("Hóa đơn giá trị gia tăng ĐẶNG NGUYỄN ộ ệ ỷ ữ ₫") >= 0) throw TemplateException.invalid("FONT_GLYPHS_MISSING", "Font không đủ ký tự tiếng Việt");
                    mime = "font/ttf";
                    metadata.put("family", font.getFamily(Locale.ROOT));
                }
                case "PDF_REFERENCE" -> {
                    try (PDDocument pdf = PDDocument.load(bytes)) {
                        if (pdf.isEncrypted() || pdf.getNumberOfPages() > properties.getMaxPages()) throw TemplateException.invalid("INVALID_PDF", "PDF mã hóa hoặc vượt giới hạn trang");
                        metadata.put("pageCount", pdf.getNumberOfPages());
                    }
                    mime = "application/pdf";
                }
                default -> throw TemplateException.invalid("INVALID_ASSET_KIND", "Loại tài nguyên không được hỗ trợ");
            }
        } catch (TemplateException ex) { throw ex; }
        catch (Exception ex) { throw TemplateException.invalid("INVALID_ASSET", "Không đọc được nội dung tài nguyên"); }
        return store(bytes, kind, mime, json(metadata), system ? "SYSTEM" : "COMPANY", system ? null : access.companyId(), access.actorId());
    }

    public InvoiceAssetEntity store(byte[] bytes, String kind, String mime, String metadata, String scope, Long companyId, Long actorId) {
        if (!Set.of("IMAGE", "FONT", "PDF_REFERENCE").contains(kind)) throw TemplateException.invalid("INVALID_ASSET_KIND", "Loại tài nguyên không được hỗ trợ");
        if (bytes.length > properties.getMaxInputBytes()) throw TemplateException.invalid("ASSET_SIZE_LIMIT", "Tài nguyên vượt giới hạn");
        InvoiceAssetEntity asset = new InvoiceAssetEntity();
        asset.setScope(scope);
        asset.setCompanyId(companyId);
        asset.setKind(kind);
        asset.setStorageKey(storage.write(bytes));
        asset.setMimeType(mime);
        asset.setSha256(DocumentStorage.sha256(bytes));
        asset.setByteSize((long) bytes.length);
        asset.setMetadataJson(metadata);
        asset.setCreatedBy(actorId);
        asset.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return repository.save(asset);
    }

    public InvoiceAssetEntity require(Long id, String targetScope, Long companyId) {
        var asset = repository.findById(id).orElseThrow(TemplateException::missing);
        if ("COMPANY".equals(asset.getScope()) && (!"COMPANY".equals(targetScope) || !Objects.equals(companyId, asset.getCompanyId()))) throw TemplateException.missing();
        return asset;
    }

    public byte[] read(InvoiceAssetEntity asset) { return storage.read(asset.getStorageKey(), asset.getSha256()); }

    public Map<String, AssetRef> manifest(String json) {
        try { return mapper.readValue(json, new TypeReference<LinkedHashMap<String, AssetRef>>() { }); }
        catch (Exception ex) { throw new IllegalStateException("Manifest tài nguyên không hợp lệ", ex); }
    }

    public Map<Long, InvoicePdfRenderer.Resource> resources(String manifest, String scope, Long companyId) {
        Map<Long, InvoicePdfRenderer.Resource> result = new LinkedHashMap<>();
        long totalBytes = 0;
        for (AssetRef ref : manifest(manifest).values()) {
            var asset = require(ref.assetId(), scope, companyId);
            totalBytes += asset.getByteSize();
            if (totalBytes > properties.getMaxInputBytes() * 4L) throw TemplateException.invalid("ASSET_SIZE_LIMIT", "Tổng tài nguyên mẫu vượt giới hạn");
            if (!asset.getSha256().equals(ref.sha256()) || !asset.getMimeType().equals(ref.mimeType())) throw TemplateException.invalid("ASSET_HASH_MISMATCH", "Tài nguyên khác bản đã công bố");
            result.put(asset.getId(), new InvoicePdfRenderer.Resource(read(asset), asset.getMimeType()));
        }
        return result;
    }

    public void pin(String manifest, String ownerType, Long ownerId) {
        for (AssetRef ref : manifest(manifest).values()) {
            if (pins.existsByAssetIdAndOwnerTypeAndOwnerId(ref.assetId(), ownerType, ownerId)) continue;
            var pin = new InvoiceAssetPinEntity();
            pin.setAssetId(ref.assetId());
            pin.setOwnerType(ownerType);
            pin.setOwnerId(ownerId);
            pin.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            pins.save(pin);
        }
    }

    public AssetRef reference(InvoiceAssetEntity asset) { return new AssetRef(asset.getId(), asset.getSha256(), asset.getMimeType()); }

    public String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
