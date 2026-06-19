package vn.hoadon.services.impl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.dto.invoiceimport.InvoiceImportDTO;
import vn.hoadon.dto.invoiceimport.InvoiceImportResultDTO;
import vn.hoadon.entity.CustomersEntity;
import vn.hoadon.entity.InvoiceImportEntity;
import vn.hoadon.entity.ProductsEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.CustomerRepository;
import vn.hoadon.repositories.InvoiceImportRepository;
import vn.hoadon.repositories.ProductRepository;
import vn.hoadon.services.CatalogImportService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CatalogImportServiceImpl implements CatalogImportService {
    private static final DataFormatter FORMATTER = new DataFormatter(Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private InvoiceImportRepository importRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public ResponseEntity<byte[]> template(String type) {
        CatalogType catalogType = resolveType(type);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (catalogType == CatalogType.CUSTOMER) {
                writeCustomerTemplate(workbook);
            } else {
                writeProductTemplate(workbook);
            }
            workbook.write(out);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment().filename(catalogType.templateFilename).build());
            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception ex) {
            throw new IllegalStateException("Không tạo được mẫu Excel " + catalogType.title.toLowerCase(Locale.ROOT) + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public InvoiceImportResultDTO importFile(String type, MultipartFile file, UserEntity user) {
        CatalogType catalogType = resolveType(type);
        requireUser(user);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file Excel để import");
        }
        String originalName = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), "import.xlsx"));
        if (!isExcel(originalName)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file Excel .xlsx hoặc .xls");
        }

        try {
            Long companyId = user.getCompanyId();
            Path dir = resolveImportDir(companyId, catalogType);
            String storedName = buildStoredFileName(originalName);
            Path target = dir.resolve(storedName).normalize();
            if (!target.startsWith(dir.normalize())) {
                throw new IllegalArgumentException("Tên file import không hợp lệ");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            InvoiceImportEntity entity = newEntity(catalogType, user, originalName, storedName, target, publicImportUrl(companyId, catalogType, storedName));
            entity = importRepository.save(entity);
            return processStoredFile(catalogType, entity, user, target);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể lưu file import: " + ex.getMessage(), ex);
        }
    }

    @Override
    public InvoiceImportResultDTO reimport(String type, Long importId, UserEntity user) {
        CatalogType catalogType = resolveType(type);
        requireUser(user);
        if (importId == null) {
            throw new IllegalArgumentException("Thiếu ID lần import");
        }
        InvoiceImportEntity source = importRepository.findByIdAndCompanyIdAndImportType(importId, user.getCompanyId(), catalogType.importType)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file import của công ty"));
        if (source.getFilePath() == null || source.getFilePath().isBlank()) {
            throw new IllegalArgumentException("Lần import này chưa có đường dẫn file");
        }
        Path filePath = Path.of(source.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File import không còn tồn tại trên hệ thống");
        }

        InvoiceImportEntity entity = new InvoiceImportEntity();
        entity.setCompanyId(user.getCompanyId());
        entity.setUserId(user.getId());
        entity.setSourceImportId(source.getId());
        entity.setImportType(catalogType.importType);
        entity.setOriginalFilename(source.getOriginalFilename());
        entity.setStoredFilename(source.getStoredFilename());
        entity.setFilePath(source.getFilePath());
        entity.setFileUrl(source.getFileUrl());
        resetProcessingCounters(entity);
        entity = importRepository.save(entity);
        return processStoredFile(catalogType, entity, user, filePath);
    }

    @Override
    public Page<InvoiceImportDTO> list(String type, UserEntity user, Pageable pageable) {
        CatalogType catalogType = resolveType(type);
        requireUser(user);
        return importRepository.findByCompanyIdAndImportTypeOrderByIdDesc(user.getCompanyId(), catalogType.importType, pageable).map(this::toDto);
    }

    private InvoiceImportResultDTO processStoredFile(CatalogType type, InvoiceImportEntity entity, UserEntity user, Path filePath) {
        try {
            List<String> errors = new ArrayList<>();
            List<String> importedCodes;
            if (type == CatalogType.CUSTOMER) {
                List<CustomerImportRow> rows = readCustomerRows(filePath, errors);
                entity.setTotalRows(rows.size());
                entity.setItemCount(rows.size());
                entity.setInvoiceCount(rows.size());
                if (!errors.isEmpty()) {
                    markError(type, entity, errors);
                    return toResult(entity);
                }
                importedCodes = saveCustomers(rows, user.getCompanyId());
            } else {
                List<ProductImportRow> rows = readProductRows(filePath, errors);
                entity.setTotalRows(rows.size());
                entity.setItemCount(rows.size());
                entity.setInvoiceCount(rows.size());
                if (!errors.isEmpty()) {
                    markError(type, entity, errors);
                    return toResult(entity);
                }
                importedCodes = saveProducts(rows, user.getCompanyId());
            }

            entity.setStatus("SUCCESS");
            entity.setSuccessCount(importedCodes.size());
            entity.setErrorCount(0);
            entity.setErrorMessage(null);
            entity.setImportedItemIds(String.join(",", importedCodes));
            entity.setImportedInvoiceIds(String.join(",", importedCodes));
            importRepository.save(entity);
            return toResult(entity);
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Import " + type.itemLabelLower + " thất bại";
            markError(type, entity, List.of(message));
            return toResult(entity);
        }
    }

    private void writeCustomerTemplate(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Import khách hàng");
        CellStyle headerStyle = headerStyle(workbook);
        String[] headers = {
                "Mã khách hàng",
                "Đơn vị mua hàng",
                "Người mua hàng",
                "Mã số thuế",
                "Địa chỉ",
                "Email",
                "Điện thoại",
                "Fax",
                "Số tài khoản ngân hàng",
                "Tại ngân hàng",
                "Ghi chú",
                "Trạng thái\n(1 Hoạt động / 0 Ngừng)"
        };
        writeHeader(sheet, headerStyle, headers);
        addDropdown(sheet, 1, 500, 11, "1", "0", "Hoạt động", "Ngừng hoạt động");
        Object[][] samples = {
                {"KH001", "Công ty TNHH Minh An", "Nguyễn Văn A", "0101234567", "12 Nguyễn Trãi, Hà Nội", "ketoan@minhan.vn", "0901000001", "", "001100000001", "VCB", "", 1},
                {"KH002", "", "Trần Thị B", "", "25 Lê Lợi, Đà Nẵng", "binhminh@example.com", "0902000002", "", "", "", "Khách lẻ", 1}
        };
        writeSamples(sheet, samples);
        writeSimpleGuide(workbook, "Hướng dẫn", "Mẫu import khách hàng", List.of(
                "- Mã khách hàng là bắt buộc và dùng để cập nhật nếu đã tồn tại trong công ty.",
                "- Bắt buộc nhập ít nhất một trong hai cột Đơn vị mua hàng hoặc Người mua hàng.",
                "- Trạng thái nhận 1/Hoạt động hoặc 0/Ngừng hoạt động; bỏ trống mặc định là 1."
        ));
    }

    private void writeProductTemplate(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Import sản phẩm");
        CellStyle headerStyle = headerStyle(workbook);
        String[] headers = {
                "Mã sản phẩm",
                "Tên sản phẩm",
                "Đơn vị tính",
                "Đơn giá",
                "Thuế suất\n(0/5/8/10/KCT/KKKNT/Khác)",
                "Mô tả",
                "Trạng thái\n(1 Đang bán / 0 Ngừng)"
        };
        writeHeader(sheet, headerStyle, headers);
        addDropdown(sheet, 1, 500, 4, "0", "5", "8", "10", "KCT", "KKKNT", "Khác", "-1", "-2", "-3");
        addDropdown(sheet, 1, 500, 6, "1", "0", "Đang bán", "Ngừng kinh doanh");
        Object[][] samples = {
                {"SP001", "Dịch vụ tư vấn", "Lần", 3000000, 10, "", 1},
                {"SP002", "Phí triển khai", "Gói", 2000000, 10, "", 1},
                {"DV001", "Dịch vụ bảo trì", "Tháng", 500000, 0, "", 1}
        };
        writeSamples(sheet, samples);
        writeSimpleGuide(workbook, "Hướng dẫn", "Mẫu import sản phẩm", List.of(
                "- Mã sản phẩm và Tên sản phẩm là bắt buộc.",
                "- Mã sản phẩm dùng để cập nhật nếu đã tồn tại trong công ty.",
                "- Thuế suất nhận 0, 5, 8, 10, KCT/-1, KKKNT/-2 hoặc Khác/-3; bỏ trống mặc định là KCT.",
                "- Trạng thái nhận 1/Đang bán hoặc 0/Ngừng kinh doanh; bỏ trống mặc định là 1."
        ));
    }

    private void writeHeader(Sheet sheet, CellStyle style, String[] headers) {
        Row header = sheet.createRow(0);
        header.setHeightInPoints(42);
        for (int i = 0; i < headers.length; i += 1) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
            int visibleLength = headers[i].replace("\n", " ").length();
            sheet.setColumnWidth(i, Math.min(10000, Math.max(4200, visibleLength * 300)));
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    private void writeSamples(Sheet sheet, Object[][] samples) {
        for (int r = 0; r < samples.length; r += 1) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < samples[r].length; c += 1) {
                Cell cell = row.createCell(c);
                Object value = samples[r][c];
                if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(value));
                }
            }
        }
    }

    private void writeSimpleGuide(Workbook workbook, String sheetName, String title, List<String> lines) {
        Sheet guide = workbook.createSheet(sheetName);
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        int rowIndex = 0;
        Row titleRow = guide.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        rowIndex++;
        for (String line : lines) {
            Row row = guide.createRow(rowIndex++);
            row.createCell(0).setCellValue(line);
        }
        guide.setColumnWidth(0, 18000);
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setWrapText(true);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return headerStyle;
    }

    private void addDropdown(Sheet sheet, int firstRow, int lastRow, int column, String... values) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, lastRow, column, column);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private List<CustomerImportRow> readCustomerRows(Path filePath, List<String> errors) throws Exception {
        List<CustomerImportRow> rows = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        try (InputStream input = Files.newInputStream(filePath); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                errors.add("File Excel không có sheet dữ liệu");
                return rows;
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i += 1) {
                Row row = sheet.getRow(i);
                if (isEmptyRow(row, 11)) continue;
                int rowNo = i + 1;
                CustomerImportRow item = new CustomerImportRow();
                item.code = text(row, 0);
                item.companyName = text(row, 1);
                item.buyerName = text(row, 2);
                item.taxCode = text(row, 3);
                item.address = text(row, 4);
                item.email = text(row, 5);
                item.phone = text(row, 6);
                item.fax = text(row, 7);
                item.bankAccountNumber = text(row, 8);
                item.bankName = text(row, 9);
                item.description = text(row, 10);
                item.status = status(row, 11, 1);
                validateCustomerRow(rowNo, item, codes, errors);
                rows.add(item);
            }
        }
        if (rows.isEmpty() && errors.isEmpty()) {
            errors.add("File Excel không có dòng dữ liệu để import");
        }
        return rows;
    }

    private List<ProductImportRow> readProductRows(Path filePath, List<String> errors) throws Exception {
        List<ProductImportRow> rows = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        try (InputStream input = Files.newInputStream(filePath); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                errors.add("File Excel không có sheet dữ liệu");
                return rows;
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i += 1) {
                Row row = sheet.getRow(i);
                if (isEmptyRow(row, 6)) continue;
                int rowNo = i + 1;
                ProductImportRow item = new ProductImportRow();
                item.code = text(row, 0);
                item.name = text(row, 1);
                item.unit = text(row, 2);
                item.price = decimal(row, 3);
                item.priceText = text(row, 3);
                item.vatRate = vatRate(row, 4);
                item.vatRateText = text(row, 4);
                item.description = text(row, 5);
                item.status = status(row, 6, 1);
                validateProductRow(rowNo, item, codes, errors);
                rows.add(item);
            }
        }
        if (rows.isEmpty() && errors.isEmpty()) {
            errors.add("File Excel không có dòng dữ liệu để import");
        }
        return rows;
    }

    private void validateCustomerRow(int rowNo, CustomerImportRow item, Set<String> codes, List<String> errors) {
        if (item.code.isBlank()) {
            errors.add("Dòng " + rowNo + ": thiếu Mã khách hàng");
        } else if (!codes.add(item.code.toLowerCase(Locale.ROOT))) {
            errors.add("Dòng " + rowNo + ": trùng Mã khách hàng trong file (" + item.code + ")");
        }
        if (item.companyName.isBlank() && item.buyerName.isBlank()) {
            errors.add("Dòng " + rowNo + ": cần nhập Đơn vị mua hàng hoặc Người mua hàng");
        }
    }

    private void validateProductRow(int rowNo, ProductImportRow item, Set<String> codes, List<String> errors) {
        if (item.code.isBlank()) {
            errors.add("Dòng " + rowNo + ": thiếu Mã sản phẩm");
        } else if (!codes.add(item.code.toLowerCase(Locale.ROOT))) {
            errors.add("Dòng " + rowNo + ": trùng Mã sản phẩm trong file (" + item.code + ")");
        }
        if (item.name.isBlank()) {
            errors.add("Dòng " + rowNo + ": thiếu Tên sản phẩm");
        }
        if (!item.priceText.isBlank() && item.price == null) {
            errors.add("Dòng " + rowNo + ": Đơn giá không đúng định dạng");
        }
        if (item.price != null && item.price.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Dòng " + rowNo + ": Đơn giá không được âm");
        }
        if (!item.vatRateText.isBlank() && item.vatRate == null) {
            errors.add("Dòng " + rowNo + ": Thuế suất không đúng định dạng");
        } else if (item.vatRate != null && !Set.of(-3, -2, -1, 0, 5, 8, 10).contains(item.vatRate)) {
            errors.add("Dòng " + rowNo + ": Thuế suất chỉ nhận 0, 5, 8, 10, KCT, KKKNT hoặc Khác");
        }
    }

    private List<String> saveCustomers(List<CustomerImportRow> rows, Long companyId) {
        List<String> importedCodes = new ArrayList<>();
        for (CustomerImportRow row : rows) {
            CustomersEntity entity = customerRepository.findByCompanyIdAndCode(companyId, row.code).stream().findFirst().orElseGet(CustomersEntity::new);
            entity.setCompanyId(companyId);
            entity.setCode(row.code);
            entity.setCompanyName(row.companyName);
            entity.setBuyerName(row.buyerName);
            entity.setTaxCode(row.taxCode);
            entity.setAddress(row.address);
            entity.setEmail(row.email);
            entity.setPhone(row.phone);
            entity.setFax(row.fax);
            entity.setBankAccountNumber(row.bankAccountNumber);
            entity.setBankName(row.bankName);
            entity.setDescription(row.description);
            entity.setStatus(row.status);
            customerRepository.save(entity);
            importedCodes.add(row.code);
        }
        return importedCodes;
    }

    private List<String> saveProducts(List<ProductImportRow> rows, Long companyId) {
        List<String> importedCodes = new ArrayList<>();
        for (ProductImportRow row : rows) {
            ProductsEntity entity = productRepository.findByCompanyIdAndCode(companyId, row.code).stream().findFirst().orElseGet(ProductsEntity::new);
            entity.setCompanyId(companyId);
            entity.setCode(row.code);
            entity.setName(row.name);
            entity.setUnit(row.unit);
            entity.setPrice(row.price != null ? row.price : BigDecimal.ZERO);
            entity.setVatRate(row.vatRate != null ? row.vatRate : -1);
            entity.setDescription(row.description);
            entity.setStatus(row.status);
            productRepository.save(entity);
            importedCodes.add(row.code);
        }
        return importedCodes;
    }

    private InvoiceImportEntity newEntity(CatalogType type, UserEntity user, String originalName, String storedName, Path filePath, String fileUrl) {
        InvoiceImportEntity entity = new InvoiceImportEntity();
        entity.setCompanyId(user.getCompanyId());
        entity.setUserId(user.getId());
        entity.setImportType(type.importType);
        entity.setOriginalFilename(originalName);
        entity.setStoredFilename(storedName);
        entity.setFilePath(filePath.toString().replace("\\", "/"));
        entity.setFileUrl(fileUrl);
        resetProcessingCounters(entity);
        return entity;
    }

    private void resetProcessingCounters(InvoiceImportEntity entity) {
        entity.setStatus("PROCESSING");
        entity.setTotalRows(0);
        entity.setInvoiceCount(0);
        entity.setItemCount(0);
        entity.setSuccessCount(0);
        entity.setErrorCount(0);
        entity.setErrorMessage(null);
        entity.setImportedInvoiceIds(null);
        entity.setImportedItemIds(null);
    }

    private void markError(CatalogType type, InvoiceImportEntity entity, List<String> errors) {
        entity.setStatus("ERROR");
        entity.setErrorCount(errors != null ? errors.size() : 1);
        entity.setSuccessCount(0);
        String message = errors == null ? "Import " + type.itemLabelLower + " thất bại" : String.join("\n", errors);
        if (message.length() > 3900) {
            message = message.substring(0, 3900) + "...";
        }
        entity.setErrorMessage(message);
        importRepository.save(entity);
    }

    private InvoiceImportDTO toDto(InvoiceImportEntity e) {
        InvoiceImportDTO dto = new InvoiceImportDTO();
        dto.id = e.getId();
        dto.companyId = e.getCompanyId();
        dto.userId = e.getUserId();
        dto.sourceImportId = e.getSourceImportId();
        dto.importType = e.getImportType();
        dto.originalFilename = e.getOriginalFilename();
        dto.storedFilename = e.getStoredFilename();
        dto.filePath = e.getFilePath();
        dto.fileUrl = e.getFileUrl();
        dto.status = e.getStatus();
        dto.totalRows = e.getTotalRows();
        dto.invoiceCount = e.getInvoiceCount();
        dto.itemCount = e.getItemCount();
        dto.successCount = e.getSuccessCount();
        dto.errorCount = e.getErrorCount();
        dto.errorMessage = e.getErrorMessage();
        dto.importedInvoiceIds = e.getImportedInvoiceIds();
        dto.importedItemIds = e.getImportedItemIds();
        dto.createdAt = e.getCreatedAt();
        dto.updatedAt = e.getUpdatedAt();
        return dto;
    }

    private InvoiceImportResultDTO toResult(InvoiceImportEntity e) {
        InvoiceImportResultDTO dto = new InvoiceImportResultDTO();
        dto.id = e.getId();
        dto.status = e.getStatus();
        dto.importType = e.getImportType();
        dto.totalRows = e.getTotalRows();
        dto.invoiceCount = e.getInvoiceCount();
        dto.itemCount = e.getItemCount();
        dto.successCount = e.getSuccessCount();
        dto.errorCount = e.getErrorCount();
        dto.errorMessage = e.getErrorMessage();
        dto.fileUrl = e.getFileUrl();
        dto.importedInvoiceIds = e.getImportedInvoiceIds();
        dto.importedItemIds = e.getImportedItemIds();
        return dto;
    }

    private void requireUser(UserEntity user) {
        if (user == null || user.getCompanyId() == null) {
            throw new IllegalArgumentException("Không xác định được công ty/người dùng");
        }
    }

    private CatalogType resolveType(String type) {
        return CatalogType.from(type).orElseThrow(() -> new IllegalArgumentException("Loại import không hợp lệ"));
    }

    private Path resolveImportDir(Long companyId, CatalogType type) throws java.io.IOException {
        Path dir = Path.of("uploads", String.valueOf(companyId), type.uploadDir);
        Files.createDirectories(dir);
        return dir;
    }

    private String publicImportUrl(Long companyId, CatalogType type, String fileName) {
        return "/uploads/" + companyId + "/" + type.uploadDir + "/" + fileName;
    }

    private String buildStoredFileName(String originalName) {
        String ext = extension(originalName);
        String base = originalName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = sanitizeFileName(base);
        return LocalDateTime.now().format(FILE_TS) + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + base + ext;
    }

    private String sanitizeFileName(String value) {
        String s = Normalizer.normalize(Objects.toString(value, "import"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_");
        if (s.isBlank()) s = "import";
        if (s.length() > 80) s = s.substring(0, 80);
        return s;
    }

    private boolean isExcel(String name) {
        String ext = extension(name).toLowerCase(Locale.ROOT);
        return ".xlsx".equals(ext) || ".xls".equals(ext);
    }

    private String extension(String name) {
        int dot = name != null ? name.lastIndexOf('.') : -1;
        return dot >= 0 ? name.substring(dot) : ".xlsx";
    }

    private boolean isEmptyRow(Row row, int lastColumn) {
        if (row == null) return true;
        for (int i = 0; i <= lastColumn; i += 1) {
            if (!text(row, i).isBlank()) return false;
        }
        return true;
    }

    private String text(Row row, int index) {
        if (row == null) return "";
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        String value = FORMATTER.formatCellValue(cell);
        return value != null ? value.trim() : "";
    }

    private Double number(Row row, int index) {
        if (row == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        } catch (Exception ignore) {}
        String s = text(row, index);
        if (s.isBlank()) return null;
        s = s.replace("\u00A0", "").replace(" ", "").replace("₫", "").replace("đ", "");
        s = s.replaceAll("[^0-9,.-]", "");
        if (s.isBlank() || "-".equals(s)) return null;
        int comma = s.lastIndexOf(',');
        int dot = s.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            s = comma > dot ? s.replace(".", "").replace(",", ".") : s.replace(",", "");
        } else if (comma >= 0) {
            s = s.replace(",", ".");
        }
        try {
            return Double.parseDouble(s);
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal decimal(Row row, int index) {
        Double value = number(row, index);
        if (value == null) return null;
        return new BigDecimal(Double.toString(value));
    }

    private Integer vatRate(Row row, int index) {
        String raw = text(row, index);
        String s = raw.toLowerCase(Locale.ROOT).trim();
        if (s.isBlank()) return -1;
        if (s.contains("kct") || s.contains("khong chiu") || s.contains("không chịu")) return -1;
        if (s.contains("kkknt") || s.contains("khong ke khai") || s.contains("không kê khai")) return -2;
        if (s.contains("khac") || s.contains("khác")) return -3;
        Double n = number(row, index);
        return n != null ? n.intValue() : -1;
    }

    private Integer status(Row row, int index, int defaultValue) {
        String s = text(row, index).toLowerCase(Locale.ROOT);
        if (s.isBlank()) return defaultValue;
        if (s.equals("0") || s.contains("ngừng") || s.contains("ngung") || s.contains("khóa") || s.contains("khoa")) {
            return 0;
        }
        return 1;
    }

    private enum CatalogType {
        CUSTOMER("customer", "CUSTOMER", "importkhachhang", "mau-import-khach-hang.xlsx", "Import khách hàng", "khách hàng"),
        PRODUCT("product", "PRODUCT", "importsanpham", "mau-import-san-pham.xlsx", "Import sản phẩm", "sản phẩm");

        final String path;
        final String importType;
        final String uploadDir;
        final String templateFilename;
        final String title;
        final String itemLabelLower;

        CatalogType(String path, String importType, String uploadDir, String templateFilename, String title, String itemLabelLower) {
            this.path = path;
            this.importType = importType;
            this.uploadDir = uploadDir;
            this.templateFilename = templateFilename;
            this.title = title;
            this.itemLabelLower = itemLabelLower;
        }

        static Optional<CatalogType> from(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(type -> type.path.equals(normalized)).findFirst();
        }
    }

    private static class CustomerImportRow {
        String code = "";
        String companyName = "";
        String buyerName = "";
        String taxCode = "";
        String address = "";
        String email = "";
        String phone = "";
        String fax = "";
        String bankAccountNumber = "";
        String bankName = "";
        String description = "";
        Integer status = 1;
    }

    private static class ProductImportRow {
        String code = "";
        String name = "";
        String unit = "";
        BigDecimal price = BigDecimal.ZERO;
        String priceText = "";
        Integer vatRate = -1;
        String vatRateText = "";
        String description = "";
        Integer status = 1;
    }
}
