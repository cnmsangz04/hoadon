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
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.InvoiceImportEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceImportRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.services.InvoiceImportService;
import vn.hoadon.services.InvoiceService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceImportServiceImpl implements InvoiceImportService {

    private static final String TYPE_INVOICE = "INVOICE";
    private static final String IMPORT_TYPE = "importhoadon";
    private static final DataFormatter FORMATTER = new DataFormatter(Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private InvoiceImportRepository repository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private FormInvoiceRepository formInvoiceRepository;

    @Autowired
    private RegisterInvoiceRepository registerInvoiceRepository;

    @Override
    public ResponseEntity<byte[]> template() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Import hóa đơn");
            CreationHelper helper = workbook.getCreationHelper();
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

            String[] headers = {
                    "Mã hóa đơn import\n(cùng mã gom thành 1 hóa đơn)",
                    "Ngày lập\n(dd/MM/yyyy)",
                    "Hình thức thanh toán\n(Tiền mặt/Chuyển khoản)",
                    "Mã khách hàng",
                    "Đơn vị mua",
                    "Người mua",
                    "Mã số thuế",
                    "Địa chỉ",
                    "Email",
                    "Điện thoại",
                    "Ngân hàng",
                    "Số tài khoản",
                    "Mã hàng",
                    "Tên hàng hóa/Dịch vụ",
                    "Đơn vị tính",
                    "Số lượng",
                    "Đơn giá",
                    "Thành tiền",
                    "Thuế suất\n(0/5/8/10/KCT/KKKNT/Khác)",
                    "Thuế suất khác\n(khi Thuế suất = Khác)",
                    "Tính chất\n(1 HH,DV; 2 KM; 3 CK; 4 Ghi chú)",
                    "Ghi chú"
            };
            Row header = sheet.createRow(0);
            header.setHeightInPoints(48);
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            for (int i = 0; i < headers.length; i += 1) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                int visibleLength = headers[i].replace("\n", " ").length();
                sheet.setColumnWidth(i, Math.min(10000, Math.max(4200, visibleLength * 300)));
            }
            sheet.setColumnWidth(0, 9500);
            sheet.setColumnWidth(18, 9500);
            sheet.setColumnWidth(19, 9000);
            sheet.setColumnWidth(20, 10000);
            addHeaderComment(sheet, helper, drawing, header.getCell(0),
                    "Các dòng cùng Mã hóa đơn import sẽ được gom thành 1 hóa đơn có nhiều dòng hàng. Đổi mã khi muốn tạo hóa đơn khác.");
            addHeaderComment(sheet, helper, drawing, header.getCell(18),
                    "Nhập 0, 5, 8, 10, KCT, KKKNT hoặc Khác. Nếu dùng mẫu một thuế suất, các dòng cùng mã phải cùng thuế suất.");
            addHeaderComment(sheet, helper, drawing, header.getCell(19),
                    "Chỉ nhập khi cột Thuế suất là Khác hoặc -3.");
            addHeaderComment(sheet, helper, drawing, header.getCell(20),
                    "1 HH,DV; 2 Khuyến mại; 3 Chiết khấu; 4 Ghi chú.");
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
            addDropdown(sheet, 1, 500, 2, "Tiền mặt", "Chuyển khoản", "Tiền mặt/Chuyển khoản", "1", "2", "3");
            addDropdown(sheet, 1, 500, 18, "0", "5", "8", "10", "KCT", "KKKNT", "Khác", "-1", "-2", "-3");
            addDropdown(sheet, 1, 500, 20, "1", "2", "3", "4");

            Object[][] samples = {
                    {"HD001", LocalDate.now(), "Chuyển khoản", "KH001", "Công ty TNHH Minh An", "Nguyễn Văn A", "0101234567", "12 Nguyễn Trãi, Hà Nội", "ketoan@minhan.vn", "0901000001", "VCB", "001100000001", "SP001", "Dịch vụ tư vấn", "Lần", 1, 3000000, "", 10, "", 1, ""},
                    {"HD001", LocalDate.now(), "Chuyển khoản", "KH001", "Công ty TNHH Minh An", "Nguyễn Văn A", "0101234567", "12 Nguyễn Trãi, Hà Nội", "ketoan@minhan.vn", "0901000001", "VCB", "001100000001", "SP002", "Phí triển khai", "Gói", 1, 2000000, "", 10, "", 1, ""},
                    {"HD002", LocalDate.now(), "Tiền mặt", "KH002", "Hộ kinh doanh Bình Minh", "Trần Thị B", "", "25 Lê Lợi, Đà Nẵng", "binhminh@example.com", "0902000002", "", "", "DV001", "Dịch vụ bảo trì", "Tháng", 2, 500000, "", 0, "", 1, ""}
            };
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
            for (int r = 0; r < samples.length; r += 1) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c += 1) {
                    Cell cell = row.createCell(c);
                    Object value = samples[r][c];
                    if (value instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else if (value instanceof LocalDate d) {
                        cell.setCellValue(java.util.Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                        cell.setCellStyle(dateStyle);
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }
            addHeaderComment(sheet, helper, drawing, sheet.getRow(1).getCell(0),
                    "HD001 có 2 dòng trong file mẫu, sau import sẽ tạo 1 hóa đơn có 2 dòng hàng.");
            addHeaderComment(sheet, helper, drawing, sheet.getRow(2).getCell(0),
                    "Dòng này vẫn thuộc hóa đơn HD001 vì có cùng Mã hóa đơn import.");
            addHeaderComment(sheet, helper, drawing, sheet.getRow(3).getCell(0),
                    "HD002 là mã khác nên sẽ tạo hóa đơn khác.");

            Sheet guide = workbook.createSheet("Hướng dẫn");
            writeGuideSheet(workbook, guide);
            guide.setColumnWidth(0, 18000);

            workbook.write(out);
            HttpHeaders headersOut = new HttpHeaders();
            headersOut.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headersOut.setContentDisposition(ContentDisposition.attachment().filename("mau-import-hoa-don.xlsx").build());
            return new ResponseEntity<>(out.toByteArray(), headersOut, HttpStatus.OK);
        } catch (Exception ex) {
            throw new IllegalStateException("Không tạo được mẫu Excel import hóa đơn: " + ex.getMessage(), ex);
        }
    }

    private void addHeaderComment(Sheet sheet, CreationHelper helper, Drawing<?> drawing, Cell cell, String text) {
        if (sheet == null || helper == null || drawing == null || cell == null || text == null || text.isBlank()) {
            return;
        }
        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(Math.min(cell.getColumnIndex() + 5, 21));
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 4);
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(helper.createRichTextString(text));
        comment.setAuthor("Hệ thống");
        cell.setCellComment(comment);
    }

    private void addDropdown(Sheet sheet, int firstRow, int lastRow, int column, String... values) {
        if (sheet == null || values == null || values.length == 0) {
            return;
        }
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, lastRow, column, column);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void writeGuideSheet(Workbook workbook, Sheet guide) {
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle sectionStyle = workbook.createCellStyle();
        Font sectionFont = workbook.createFont();
        sectionFont.setBold(true);
        sectionStyle.setFont(sectionFont);
        sectionStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int row = 0;
        row = writeGuideRow(guide, row, "Mẫu import hóa đơn", titleStyle);
        row++;
        row = writeGuideRow(guide, row, "Cách hiểu dữ liệu", sectionStyle);
        row = writeGuideRow(guide, row, "- Một dòng là một hàng hóa/dịch vụ.", null);
        row = writeGuideRow(guide, row, "- Các dòng cùng Mã hóa đơn import sẽ được gom thành 1 hóa đơn có nhiều dòng hàng.", null);
        row = writeGuideRow(guide, row, "- Nếu muốn tạo hóa đơn khác, hãy nhập Mã hóa đơn import khác.", null);
        row = writeGuideRow(guide, row, "- Ví dụ trong mẫu: HD001 có 2 dòng hàng nên import ra 1 hóa đơn có 2 dòng hàng; HD002 là hóa đơn khác.", null);
        row = writeGuideRow(guide, row, "- Sheet Import hóa đơn có sẵn dữ liệu mẫu hợp lệ cho cả mẫu một thuế suất và mẫu nhiều thuế suất.", null);
        row++;
        row = writeGuideRow(guide, row, "Khi công ty dùng mẫu một thuế suất", sectionStyle);
        row = writeGuideRow(guide, row, "- Các dòng cùng Mã hóa đơn import phải có cùng Thuế suất và cùng Thuế suất khác.", null);
        row = writeGuideRow(guide, row, "- Ví dụ đúng: HD001 có 2 dòng và cả 2 dòng đều Thuế suất = 10.", null);
        row = writeGuideRow(guide, row, "- Ví dụ sai: HD001 dòng 1 Thuế suất = 10, dòng 2 Thuế suất = 8.", null);
        row++;
        row = writeGuideRow(guide, row, "Khi công ty dùng mẫu nhiều thuế suất", sectionStyle);
        row = writeGuideRow(guide, row, "- Cùng một Mã hóa đơn import có thể có nhiều Thuế suất khác nhau.", null);
        row = writeGuideRow(guide, row, "- Ví dụ: một dòng dịch vụ 10% và một dòng hàng hóa 8% trong cùng một hóa đơn.", null);
        row++;
        row = writeGuideRow(guide, row, "Giá trị thường dùng", sectionStyle);
        row = writeGuideRow(guide, row, "- Ngày lập: yyyy-MM-dd hoặc dd/MM/yyyy; bỏ trống sẽ lấy ngày hiện tại.", null);
        row = writeGuideRow(guide, row, "- Hình thức thanh toán: Tiền mặt, Chuyển khoản, Tiền mặt/Chuyển khoản hoặc 1, 2, 3.", null);
        row = writeGuideRow(guide, row, "- Thuế suất: 0, 5, 8, 10; KCT hoặc -1; KKKNT hoặc -2; Khác hoặc -3 và nhập thêm Thuế suất khác.", null);
        row = writeGuideRow(guide, row, "- Tính chất: 1 HH,DV; 2 Khuyến mại; 3 Chiết khấu; 4 Ghi chú.", null);
        writeGuideRow(guide, row, "- Thành tiền có thể để trống, hệ thống tự tính Số lượng x Đơn giá.", null);
    }

    private int writeGuideRow(Sheet sheet, int rowIndex, String text, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        if (style != null) {
            cell.setCellStyle(style);
        }
        return rowIndex + 1;
    }

    @Override
    public InvoiceImportResultDTO importFile(MultipartFile file, UserEntity user) {
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
            Path dir = resolveImportDir(companyId);
            String storedName = buildStoredFileName(originalName);
            Path target = dir.resolve(storedName).normalize();
            if (!target.startsWith(dir.normalize())) {
                throw new IllegalArgumentException("Tên file import không hợp lệ");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            InvoiceImportEntity entity = new InvoiceImportEntity();
            entity.setCompanyId(companyId);
            entity.setUserId(user.getId());
            entity.setImportType(TYPE_INVOICE);
            entity.setOriginalFilename(originalName);
            entity.setStoredFilename(storedName);
            entity.setFilePath(target.toString().replace("\\", "/"));
            entity.setFileUrl(publicImportUrl(companyId, storedName));
            entity.setStatus("PROCESSING");
            entity.setTotalRows(0);
            entity.setInvoiceCount(0);
            entity.setSuccessCount(0);
            entity.setErrorCount(0);
            entity = repository.save(entity);
            return processStoredFile(entity, user, target);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể lưu file import: " + ex.getMessage(), ex);
        }
    }

    @Override
    public InvoiceImportResultDTO reimport(Long importId, UserEntity user) {
        requireUser(user);
        if (importId == null) {
            throw new IllegalArgumentException("Thiếu ID lần import");
        }
        InvoiceImportEntity source = repository.findInvoiceImportByIdAndCompanyId(importId, user.getCompanyId())
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
        entity.setImportType(TYPE_INVOICE);
        entity.setSourceImportId(source.getId());
        entity.setOriginalFilename(source.getOriginalFilename());
        entity.setStoredFilename(source.getStoredFilename());
        entity.setFilePath(source.getFilePath());
        entity.setFileUrl(source.getFileUrl());
        entity.setStatus("PROCESSING");
        entity.setTotalRows(0);
        entity.setInvoiceCount(0);
        entity.setSuccessCount(0);
        entity.setErrorCount(0);
        entity = repository.save(entity);
        return processStoredFile(entity, user, filePath);
    }

    @Override
    public Page<InvoiceImportDTO> list(UserEntity user, Pageable pageable) {
        requireUser(user);
        return repository.findInvoiceImportsByCompanyId(user.getCompanyId(), pageable).map(this::toDto);
    }

    private InvoiceImportResultDTO processStoredFile(InvoiceImportEntity entity, UserEntity user, Path filePath) {
        try {
            FormInvoiceEntity form = resolveActiveVatForm(user.getCompanyId());
            List<String> errors = new ArrayList<>();
            List<ImportInvoiceGroup> groups = readGroups(filePath, errors);
            validateOneVatRateForm(form, groups, errors);
            entity.setTotalRows(groups.stream().mapToInt(g -> g.rows.size()).sum());
            entity.setInvoiceCount(groups.size());
            entity.setItemCount(groups.size());
            if (!errors.isEmpty()) {
                markError(entity, errors);
                return toResult(entity);
            }

            List<String> lookupCodes = new ArrayList<>();
            for (ImportInvoiceGroup group : groups) {
                InvoiceService.InvoicePayload payload = group.toPayload(form.getId());
                InvoiceEntity saved = invoiceService.create(payload, user.getCompanyId(), user.getId());
                if (saved == null || saved.getId() == null) {
                    errors.add("Không lưu được hóa đơn import mã " + group.importCode);
                } else {
                    lookupCodes.add(saved.getLookupCode());
                }
            }
            if (!errors.isEmpty()) {
                markError(entity, errors);
                return toResult(entity);
            }
            entity.setStatus("SUCCESS");
            entity.setSuccessCount(lookupCodes.size());
            entity.setErrorCount(0);
            entity.setImportedInvoiceIds(String.join(",", lookupCodes));
            entity.setImportedItemIds(String.join(",", lookupCodes));
            entity.setErrorMessage(null);
            repository.save(entity);
            return toResult(entity);
        } catch (Exception ex) {
            markError(entity, List.of(ex.getMessage() != null ? ex.getMessage() : "Import hóa đơn thất bại"));
            return toResult(entity);
        }
    }

    private List<ImportInvoiceGroup> readGroups(Path filePath, List<String> errors) throws Exception {
        Map<String, ImportInvoiceGroup> map = new LinkedHashMap<>();
        try (InputStream input = Files.newInputStream(filePath); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                errors.add("File Excel không có sheet dữ liệu");
                return List.of();
            }
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i += 1) {
                Row row = sheet.getRow(i);
                if (isEmptyRow(row)) continue;
                int rowNo = i + 1;
                String importCode = text(row, 0);
                if (importCode.isBlank()) {
                    errors.add("Dòng " + rowNo + ": thiếu Mã hóa đơn import");
                    continue;
                }
                String productName = text(row, 13);
                if (productName.isBlank()) {
                    errors.add("Dòng " + rowNo + ": thiếu Tên hàng hóa/Dịch vụ");
                    continue;
                }

                ImportInvoiceGroup group = map.computeIfAbsent(importCode, key -> {
                    ImportInvoiceGroup g = new ImportInvoiceGroup();
                    g.importCode = key;
                    g.dateExport = date(row, 1);
                    g.paymentType = paymentType(row, 2);
                    g.customer = new LinkedHashMap<>();
                    g.customer.put("code", text(row, 3));
                    g.customer.put("name", text(row, 4));
                    g.customer.put("buyer", text(row, 5));
                    g.customer.put("taxcode", text(row, 6));
                    g.customer.put("address", text(row, 7));
                    g.customer.put("email", text(row, 8));
                    g.customer.put("phone", text(row, 9));
                    g.customer.put("bank_name", text(row, 10));
                    g.customer.put("bank_no", text(row, 11));
                    return g;
                });
                if (group.dateExport == null) group.dateExport = date(row, 1);
                if (group.dateExport == null) group.dateExport = LocalDate.now();
                if (group.paymentType == null) group.paymentType = paymentType(row, 2);
                if (group.paymentType == null) group.paymentType = 1;

                Double quantity = number(row, 15);
                Double price = number(row, 16);
                Double total = number(row, 17);
                if (quantity == null) quantity = 1d;
                if (price == null) price = 0d;
                if (total == null) total = quantity * price;

                Short vatRate = vatRate(row, 18);
                Integer vatRateOther = integer(row, 19);
                if (vatRate == null) vatRate = 10;
                if (vatRate == -3 && vatRateOther == null) vatRateOther = 0;
                double effectiveRate = vatRate == -3 ? safeNumber(vatRateOther) : safeNumber(vatRate);
                double vatAmount = effectiveRate < 0 ? 0d : Math.round(total * effectiveRate / 100d);
                double amount = total + vatAmount;

                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("num", group.rows.size() + 1);
                detail.put("isNum", true);
                detail.put("hidden", false);
                detail.put("disable", false);
                detail.put("name", productName);
                detail.put("code", text(row, 12));
                detail.put("unit", text(row, 14));
                detail.put("quantity", quantity);
                detail.put("price", price);
                detail.put("total", total);
                detail.put("vatRate", vatRate);
                detail.put("vatRateOther", vatRateOther != null ? vatRateOther : 0);
                detail.put("vatAmount", vatAmount);
                detail.put("amount", amount);
                detail.put("feature", feature(row, 20));
                String note = text(row, 21);
                if (!note.isBlank()) detail.put("note", note);
                group.rows.add(detail);
            }
        }
        if (map.isEmpty() && errors.isEmpty()) {
            errors.add("File Excel không có dòng dữ liệu để import");
        }
        return new ArrayList<>(map.values());
    }

    private void validateOneVatRateForm(FormInvoiceEntity form, List<ImportInvoiceGroup> groups, List<String> errors) {
        if (form == null || form.getType() == null || form.getType() != 1 || groups == null) {
            return;
        }
        for (ImportInvoiceGroup group : groups) {
            String firstRate = null;
            for (Map<String, Object> row : group.rows) {
                String currentRate = vatRateKey(row);
                if (firstRate == null) {
                    firstRate = currentRate;
                    continue;
                }
                if (!Objects.equals(firstRate, currentRate)) {
                    errors.add("Mã import " + group.importCode + ": mẫu một thuế suất chỉ được nhập một mức thuế suất");
                    break;
                }
            }
        }
    }

    private String vatRateKey(Map<String, Object> row) {
        if (row == null) return "-1:0";
        Number vatRate = row.get("vatRate") instanceof Number ? (Number) row.get("vatRate") : null;
        Number vatRateOther = row.get("vatRateOther") instanceof Number ? (Number) row.get("vatRateOther") : null;
        int rate = vatRate != null ? vatRate.intValue() : -1;
        int other = rate == -3 && vatRateOther != null ? vatRateOther.intValue() : 0;
        return rate + ":" + other;
    }

    private FormInvoiceEntity resolveActiveVatForm(Long companyId) throws Exception {
        FormInvoiceEntity activeVatForm = formInvoiceRepository.findTopByCompanyIdAndStatusAndCategoryOrderByUpdatedAtDesc(companyId, 1, 1);
        if (activeVatForm == null) {
            throw new IllegalArgumentException("Chưa có mẫu hóa đơn GTGT được kích hoạt");
        }
        List<RegisterInvoiceEntity> acceptedList = registerInvoiceRepository.findLatestAccepted(companyId, org.springframework.data.domain.PageRequest.of(0, 1));
        RegisterInvoiceEntity latestAccepted = acceptedList != null && !acceptedList.isEmpty() ? acceptedList.get(0) : null;
        if (latestAccepted == null) {
            throw new IllegalArgumentException("Chưa có tờ khai được chấp nhận");
        }
        if (activeVatForm.getCategory() != null && activeVatForm.getCategory() == 1) {
            List<String> invoiceTypes = safeList(latestAccepted.getInvoiceTypes());
            if (!containsToken(invoiceTypes, "HDGTGT")) {
                throw new IllegalArgumentException("Tờ khai không đăng ký loại hóa đơn GTGT");
            }
        }
        List<String> invoiceForms = safeList(latestAccepted.getInvoiceForms());
        Integer haveCode = activeVatForm.getHaveCode();
        boolean requireCMa = haveCode != null && haveCode == 1;
        boolean okForm = requireCMa ? containsToken(invoiceForms, "CMa") : containsToken(invoiceForms, "KCMa");
        if (!okForm) {
            throw new IllegalArgumentException(requireCMa ? "Tờ khai không đăng ký hình thức Cấp mã" : "Tờ khai không đăng ký hình thức Không cấp mã");
        }
        return activeVatForm;
    }

    private void markError(InvoiceImportEntity entity, List<String> errors) {
        entity.setStatus("ERROR");
        entity.setErrorCount(errors != null ? errors.size() : 1);
        entity.setSuccessCount(0);
        String message = errors == null ? "Import hóa đơn thất bại" : String.join("\n", errors);
        if (message.length() > 3900) {
            message = message.substring(0, 3900) + "...";
        }
        entity.setErrorMessage(message);
        repository.save(entity);
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

    private String buildStoredFileName(String originalName) {
        String ext = extension(originalName);
        String base = originalName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = sanitizeFileName(base);
        return LocalDateTime.now().format(FILE_TS) + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + base + ext;
    }

    private Path resolveImportDir(Long companyId) throws java.io.IOException {
        Path dir = Path.of("uploads", String.valueOf(companyId), IMPORT_TYPE);
        Files.createDirectories(dir);
        return dir;
    }

    private String publicImportUrl(Long companyId, String fileName) {
        return "/uploads/" + companyId + "/" + IMPORT_TYPE + "/" + fileName;
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

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (int i = 0; i <= 21; i += 1) {
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

    private LocalDate date(Row row, int index) {
        if (row == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
        } catch (Exception ignore) {}
        String s = text(row, index);
        if (s.isBlank()) return null;
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy")
        );
        for (DateTimeFormatter format : formats) {
            try {
                return LocalDate.parse(s, format);
            } catch (Exception ignore) {}
        }
        return null;
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

    private Integer integer(Row row, int index) {
        Double n = number(row, index);
        return n != null ? n.intValue() : null;
    }

    private Short vatRate(Row row, int index) {
        String raw = text(row, index);
        String s = raw.toLowerCase(Locale.ROOT).trim();
        if (s.isBlank()) return null;
        if (s.contains("kct") || s.contains("khong chiu") || s.contains("không chịu")) return -1;
        if (s.contains("kkknt") || s.contains("khong ke khai") || s.contains("không kê khai")) return -2;
        if (s.contains("khac") || s.contains("khác")) return -3;
        Double n = number(row, index);
        return n != null ? n.shortValue() : null;
    }

    private Short paymentType(Row row, int index) {
        String s = text(row, index).toLowerCase(Locale.ROOT);
        if (s.isBlank()) return 1;
        if (s.contains("3") || (s.contains("tiền mặt") && s.contains("chuyển")) || (s.contains("tien mat") && s.contains("chuyen"))) return 3;
        if (s.contains("2") || s.contains("chuyển") || s.contains("chuyen")) return 2;
        return 1;
    }

    private Integer feature(Row row, int index) {
        String s = text(row, index).toLowerCase(Locale.ROOT);
        if (s.contains("km") || s.contains("khuyến") || s.contains("khuyen")) return 2;
        if (s.contains("ck") || s.contains("chiết") || s.contains("chiet")) return 3;
        if (s.contains("ghi")) return 4;
        Integer n = integer(row, index);
        if (n != null && n >= 1 && n <= 4) return n;
        return 1;
    }

    private double safeNumber(Number n) {
        return n != null ? n.doubleValue() : 0d;
    }

    private static boolean containsToken(List<String> arr, String token) {
        if (arr == null || token == null) return false;
        for (String s : arr) {
            if (s != null && s.trim().equalsIgnoreCase(token)) return true;
        }
        return false;
    }

    private static List<String> safeList(Object o) {
        if (o == null) return Collections.emptyList();
        if (o instanceof List<?>) {
            List<?> raw = (List<?>) o;
            return raw.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
        }
        String s = String.valueOf(o).trim();
        if (s.isBlank()) return Collections.emptyList();
        if (s.startsWith("[") && s.endsWith("]")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(s, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            } catch (Exception ignore) {}
        }
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isBlank()).collect(Collectors.toList());
    }

    private String numberToWordsVn(double num) {
        long value = Math.round(Math.abs(num));
        if (value == 0) return "Không đồng";
        String[] units = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
        String[] scales = {"", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"};
        List<Integer> groups = new ArrayList<>();
        while (value > 0) {
            groups.add((int) (value % 1000));
            value /= 1000;
        }
        List<String> words = new ArrayList<>();
        for (int i = groups.size() - 1; i >= 0; i -= 1) {
            int group = groups.get(i);
            if (group == 0) continue;
            String part = readTriple(group, units);
            if (!part.isBlank()) {
                words.add(part);
                if (i < scales.length && !scales[i].isBlank()) words.add(scales[i]);
            }
        }
        String text = String.join(" ", words).replaceAll("\\s+", " ").trim();
        if (text.isBlank()) return "Không đồng";
        text = text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
        return (num < 0 ? "Âm " : "") + text + " đồng";
    }

    private String readTriple(int n, String[] units) {
        int hundred = n / 100;
        int ten = (n % 100) / 10;
        int one = n % 10;
        List<String> words = new ArrayList<>();
        if (hundred > 0) {
            words.add(units[hundred] + " trăm");
            if (ten == 0 && one > 0) words.add("lẻ");
        }
        if (ten > 1) {
            words.add(units[ten] + " mươi");
            if (one == 1) words.add("mốt");
            else if (one == 4) words.add("tư");
            else if (one == 5) words.add("lăm");
            else if (one > 0) words.add(units[one]);
        } else if (ten == 1) {
            words.add("mười");
            if (one == 5) words.add("lăm");
            else if (one > 0) words.add(units[one]);
        } else if (ten == 0 && hundred == 0 && one > 0) {
            words.add(units[one]);
        } else if (ten == 0 && one > 0) {
            words.add(units[one]);
        }
        return String.join(" ", words);
    }

    private class ImportInvoiceGroup {
        String importCode;
        LocalDate dateExport;
        Short paymentType;
        Map<String, Object> customer;
        List<Map<String, Object>> rows = new ArrayList<>();

        InvoiceService.InvoicePayload toPayload(Long formId) {
            double total = rows.stream()
                    .filter(r -> Objects.equals(Number.class.cast(r.get("feature")).intValue(), 1))
                    .mapToDouble(r -> safeNumber((Number) r.get("total")))
                    .sum();
            double vatAmount = rows.stream()
                    .filter(r -> Objects.equals(Number.class.cast(r.get("feature")).intValue(), 1))
                    .mapToDouble(r -> safeNumber((Number) r.get("vatAmount")))
                    .sum();
            double amount = rows.stream()
                    .filter(r -> Objects.equals(Number.class.cast(r.get("feature")).intValue(), 1))
                    .mapToDouble(r -> safeNumber((Number) r.get("amount")))
                    .sum();
            InvoiceService.InvoicePayload payload = new InvoiceService.InvoicePayload();
            payload.formId = formId;
            payload.no = null;
            payload.dateExport = dateExport != null ? dateExport : LocalDate.now();
            payload.paymentType = paymentType != null ? paymentType : 1;
            payload.status = 0;
            payload.orderCode = importCode;
            payload.customer = customer;
            payload.detail = rows;
            payload.total = total;
            payload.vatAmount = vatAmount;
            payload.amount = amount;
            payload.amountInWords = numberToWordsVn(amount);
            payload.currency = "VND";
            payload.exchangeRate = 1d;
            payload.vatRate = rows.isEmpty() ? -1 : ((Number) rows.get(0).get("vatRate")).shortValue();
            payload.vatRateOther = rows.isEmpty() ? 0 : ((Number) rows.get(0).get("vatRateOther")).intValue();
            return payload;
        }
    }
}
