package vn.hoadon.services.impl;

import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.dto.invoicepackage.InvoicePackageFilterDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageMonthlyStatisticDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseFilterDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageRequestDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageResponseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageStatisticsDTO;
import vn.hoadon.entity.BuyInvoiceEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.InvoicePackageEntity;
import vn.hoadon.entity.InvoicePackagePurchaseEntity;
import vn.hoadon.entity.MailTemplateEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.messaging.MailJobMessage;
import vn.hoadon.repositories.BuyInvoiceRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.InvoicePackagePurchaseRepository;
import vn.hoadon.repositories.InvoicePackageRepository;
import vn.hoadon.repositories.MailTemplateRepository;
import vn.hoadon.services.BuyInvoiceHistoryService;
import vn.hoadon.services.InvoicePackageService;
import vn.hoadon.services.MailQueueService;
import vn.hoadon.util.SystemMail;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoicePackageServiceImpl implements InvoicePackageService {

    private static final Logger log = LoggerFactory.getLogger(InvoicePackageServiceImpl.class);
    private static final String MAIL_KEY = "BUY_INVOICE_MAIL";

    @Autowired private InvoicePackageRepository packageRepository;
    @Autowired private InvoicePackagePurchaseRepository purchaseRepository;
    @Autowired private BuyInvoiceRepository buyInvoiceRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private MailTemplateRepository mailTemplateRepository;
    @Autowired private MailQueueService mailQueueService;
    @Autowired private BuyInvoiceHistoryService buyInvoiceHistoryService;

    @Override
    public Page<InvoicePackageResponseDTO> listPackages(InvoicePackageFilterDTO filter, Pageable pageable) {
        ensureDefaultPackage();
        Specification<InvoicePackageEntity> spec = packageSpec(filter);
        return packageRepository.findAll(spec, pageable).map(this::toPackageDto);
    }

    @Override
    public List<InvoicePackageResponseDTO> listActivePackages() {
        ensureDefaultPackage();
        return packageRepository.findByStatusOrderByDisplayOrderAscIdAsc(1)
                .stream()
                .map(this::toPackageDto)
                .toList();
    }

    @Override
    @Transactional
    public InvoicePackageResponseDTO savePackage(InvoicePackageRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu gói hóa đơn");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên gói hóa đơn không được để trống");
        }
        if (dto.getInvoiceQuantity() == null || dto.getInvoiceQuantity() <= 0) {
            throw new IllegalArgumentException("Số hóa đơn phải lớn hơn 0");
        }
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Đơn giá không hợp lệ");
        }

        InvoicePackageEntity entity = dto.getId() != null
                ? packageRepository.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói hóa đơn"))
                : new InvoicePackageEntity();

        entity.setName(dto.getName().trim());
        entity.setInvoiceQuantity(dto.getInvoiceQuantity());
        entity.setUnitPrice(money(dto.getUnitPrice()));
        entity.setTotalPrice(resolveTotalPrice(dto));
        entity.setIncludeTrial(Boolean.TRUE.equals(dto.getIncludeTrial()));
        entity.setDescription(trimToNull(dto.getDescription()));
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);

        return toPackageDto(packageRepository.save(entity));
    }

    @Override
    public void deletePackage(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Thiếu ID gói hóa đơn");
        }
        InvoicePackageEntity entity = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói hóa đơn"));
        entity.setStatus(0);
        packageRepository.save(entity);
    }

    @Override
    @Transactional
    public InvoicePackagePurchaseDTO purchase(Long packageId, String paymentMethod, UserEntity user) {
        if (user == null || user.getCompanyId() == null) {
            throw new IllegalArgumentException("Không xác định được công ty mua gói");
        }
        if (packageId == null) {
            throw new IllegalArgumentException("Vui lòng chọn gói hóa đơn");
        }

        InvoicePackageEntity invoicePackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói hóa đơn"));
        if (!Integer.valueOf(1).equals(invoicePackage.getStatus())) {
            throw new IllegalArgumentException("Gói hóa đơn chưa được kích hoạt");
        }

        CompanyEntity company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty"));

        String method = normalizePaymentMethod(paymentMethod);
        LocalDateTime now = LocalDateTime.now();
        InvoicePackagePurchaseEntity purchase = new InvoicePackagePurchaseEntity();
        purchase.setPackageId(invoicePackage.getId());
        purchase.setPackageName(invoicePackage.getName());
        purchase.setCompanyId(company.getId());
        purchase.setCompanyName(company.getName());
        purchase.setCompanyTaxcode(company.getTaxcode());
        purchase.setBuyerName(resolveBuyerName(user, company));
        purchase.setBuyerEmail(resolveBuyerEmail(user, company));
        purchase.setBuyerPhone(resolveBuyerPhone(user, company));
        purchase.setInvoiceQuantity(invoicePackage.getInvoiceQuantity());
        purchase.setUnitPrice(money(invoicePackage.getUnitPrice()));
        purchase.setTotalPrice(money(invoicePackage.getTotalPrice()));
        purchase.setPaymentMethod(method);
        purchase.setPaymentStatus("SUCCESS");
        purchase.setPaymentCode(fakePaymentCode(method));
        purchase.setPaidAt(now);
        purchase.setNote("Thanh toán giả lập thành công");
        purchase = purchaseRepository.save(purchase);

        BuyInvoiceEntity beforeBuyInvoice = findCurrentBuyInvoice(company.getId()).map(this::snapshot).orElse(null);
        BuyInvoiceEntity buyInvoice = upsertBuyInvoice(company.getId(), invoicePackage.getInvoiceQuantity());
        purchase.setBuyInvoiceId(buyInvoice.getId());
        purchase = purchaseRepository.save(purchase);

        buyInvoiceHistoryService.record(
                beforeBuyInvoice,
                snapshot(buyInvoice),
                "PACKAGE_PURCHASE",
                "CUSTOMER",
                user,
                purchase.getId(),
                invoicePackage.getName(),
                purchase.getPaymentCode(),
                "Khách hàng mua gói hóa đơn"
        );

        if (Integer.valueOf(2).equals(company.getStatus())) {
            company.setStatus(1);
            companyRepository.save(company);
        }

        enqueuePurchaseMail(purchase, buyInvoice, company, user);

        InvoicePackagePurchaseDTO dto = toPurchaseDto(purchase);
        dto.setCompanyStatus(company.getStatus());
        dto.setTotalInvoices(valueOrZero(buyInvoice.getAmount()));
        dto.setUsedInvoices(valueOrZero(buyInvoice.getAmountUsed()));
        dto.setRemainingInvoices(valueOrZero(buyInvoice.getAmount()) - valueOrZero(buyInvoice.getAmountUsed()));
        return dto;
    }

    @Override
    public Page<InvoicePackagePurchaseDTO> listPurchases(InvoicePackagePurchaseFilterDTO filter, Pageable pageable) {
        return purchaseRepository.findAll(purchaseSpec(filter, false), pageable).map(this::toPurchaseDto);
    }

    @Override
    public Page<InvoicePackagePurchaseDTO> listMyPurchases(Long companyId, Pageable pageable) {
        return purchaseRepository.findByCompanyId(companyId, pageable).map(this::toPurchaseDto);
    }

    @Override
    public InvoicePackageStatisticsDTO statistics(InvoicePackagePurchaseFilterDTO filter) {
        InvoicePackagePurchaseFilterDTO normalized = normalizeStatsFilter(filter);
        List<InvoicePackagePurchaseEntity> purchases = purchaseRepository.findAll(
                purchaseSpec(normalized, true),
                Sort.by("createdAt").ascending()
        );

        InvoicePackageStatisticsDTO stats = new InvoicePackageStatisticsDTO();
        stats.setTotalOrders((long) purchases.size());

        int totalInvoices = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, InvoicePackageMonthlyStatisticDTO> byMonth = initMonthMap(normalized);

        for (InvoicePackagePurchaseEntity purchase : purchases) {
            int quantity = valueOrZero(purchase.getInvoiceQuantity());
            BigDecimal revenue = purchase.getTotalPrice() != null ? purchase.getTotalPrice() : BigDecimal.ZERO;
            totalInvoices += quantity;
            totalRevenue = totalRevenue.add(revenue);

            LocalDateTime time = purchase.getPaidAt() != null ? purchase.getPaidAt() : purchase.getCreatedAt();
            String month = time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM")) : YearMonth.now().toString();
            InvoicePackageMonthlyStatisticDTO item = byMonth.computeIfAbsent(month, key -> emptyMonth(key));
            item.setOrderCount(valueOrZero(item.getOrderCount()) + 1);
            item.setInvoiceQuantity(valueOrZero(item.getInvoiceQuantity()) + quantity);
            item.setRevenue(item.getRevenue().add(revenue));
        }

        stats.setTotalInvoices(totalInvoices);
        stats.setTotalRevenue(totalRevenue);
        stats.setMonthly(new ArrayList<>(byMonth.values()));
        return stats;
    }

    @Override
    public byte[] exportPurchases(InvoicePackagePurchaseFilterDTO filter) {
        List<InvoicePackagePurchaseEntity> purchases = purchaseRepository.findAll(
                purchaseSpec(filter, false),
                Sort.by("createdAt").descending()
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bao cao mua goi");
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            String[] headers = {
                    "STT", "Mã giao dịch", "Công ty", "Mã số thuế", "Gói hóa đơn",
                    "Số hóa đơn", "Đơn giá", "Thành tiền", "Thanh toán", "Trạng thái", "Ngày thanh toán"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowIdx = 1;
            for (int i = 0; i < purchases.size(); i++) {
                InvoicePackagePurchaseEntity purchase = purchases.get(i);
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(nullToBlank(purchase.getPaymentCode()));
                row.createCell(2).setCellValue(nullToBlank(purchase.getCompanyName()));
                row.createCell(3).setCellValue(nullToBlank(purchase.getCompanyTaxcode()));
                row.createCell(4).setCellValue(nullToBlank(purchase.getPackageName()));
                row.createCell(5).setCellValue(valueOrZero(purchase.getInvoiceQuantity()));
                row.createCell(6).setCellValue(toDouble(purchase.getUnitPrice()));
                row.getCell(6).setCellStyle(moneyStyle);
                row.createCell(7).setCellValue(toDouble(purchase.getTotalPrice()));
                row.getCell(7).setCellStyle(moneyStyle);
                row.createCell(8).setCellValue(nullToBlank(purchase.getPaymentMethod()));
                row.createCell(9).setCellValue("SUCCESS".equals(purchase.getPaymentStatus()) ? "Thành công" : nullToBlank(purchase.getPaymentStatus()));
                row.createCell(10).setCellValue(purchase.getPaidAt() != null ? purchase.getPaidAt().format(formatter) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể xuất báo cáo Excel", e);
        }
    }

    private Specification<InvoicePackageEntity> packageSpec(InvoicePackageFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                    String like = "%" + filter.getKeyword().trim() + "%";
                    predicates.add(cb.like(root.get("name"), like));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<InvoicePackagePurchaseEntity> purchaseSpec(InvoicePackagePurchaseFilterDTO filter, boolean onlySuccessWhenBlank) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter != null) {
                if (filter.getCompanyId() != null) {
                    predicates.add(cb.equal(root.get("companyId"), filter.getCompanyId()));
                }
                if (filter.getPaymentMethod() != null && !filter.getPaymentMethod().isBlank()) {
                    predicates.add(cb.equal(root.get("paymentMethod"), filter.getPaymentMethod().trim().toUpperCase(Locale.ROOT)));
                }
                if (filter.getPaymentStatus() != null && !filter.getPaymentStatus().isBlank()) {
                    predicates.add(cb.equal(root.get("paymentStatus"), filter.getPaymentStatus().trim().toUpperCase(Locale.ROOT)));
                } else if (onlySuccessWhenBlank) {
                    predicates.add(cb.equal(root.get("paymentStatus"), "SUCCESS"));
                }
                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate().atStartOfDay()));
                }
                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThan(root.get("createdAt"), filter.getToDate().plusDays(1).atStartOfDay()));
                }
                if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                    String like = "%" + filter.getKeyword().trim() + "%";
                    predicates.add(cb.or(
                            cb.like(root.get("companyName"), like),
                            cb.like(root.get("companyTaxcode"), like),
                            cb.like(root.get("packageName"), like),
                            cb.like(root.get("paymentCode"), like)
                    ));
                }
            } else if (onlySuccessWhenBlank) {
                predicates.add(cb.equal(root.get("paymentStatus"), "SUCCESS"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BuyInvoiceEntity upsertBuyInvoice(Long companyId, Integer invoiceQuantity) {
        BuyInvoiceEntity buyInvoice = findCurrentBuyInvoice(companyId).orElseGet(BuyInvoiceEntity::new);
        if (buyInvoice.getId() == null) {
            buyInvoice.setCompanyId(companyId);
            buyInvoice.setCreatedAt(LocalDateTime.now());
            buyInvoice.setAmount(0);
            buyInvoice.setAmountUsed(0);
        }
        buyInvoice.setStatus(1);
        buyInvoice.setAmount(valueOrZero(buyInvoice.getAmount()) + valueOrZero(invoiceQuantity));
        buyInvoice.setAmountUsed(valueOrZero(buyInvoice.getAmountUsed()));
        buyInvoice.setUpdatedAt(LocalDateTime.now());
        return buyInvoiceRepository.save(buyInvoice);
    }

    private Optional<BuyInvoiceEntity> findCurrentBuyInvoice(Long companyId) {
        Optional<BuyInvoiceEntity> active = buyInvoiceRepository.findFirstByCompanyIdAndStatusOrderByIdDesc(companyId, 1);
        return active.isPresent() ? active : buyInvoiceRepository.findFirstByCompanyIdOrderByIdDesc(companyId);
    }

    private BuyInvoiceEntity snapshot(BuyInvoiceEntity source) {
        if (source == null) return null;
        BuyInvoiceEntity copy = new BuyInvoiceEntity();
        copy.setId(source.getId());
        copy.setCompanyId(source.getCompanyId());
        copy.setAmount(source.getAmount());
        copy.setAmountUsed(source.getAmountUsed());
        copy.setStatus(source.getStatus());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private void enqueuePurchaseMail(InvoicePackagePurchaseEntity purchase, BuyInvoiceEntity buyInvoice,
                                     CompanyEntity company, UserEntity user) {
        String toEmail = firstNotBlank(company.getContactMail(), company.getEmail(), user.getEmail());
        if (toEmail == null) {
            return;
        }
        try {
            ensureBuyInvoiceMailTemplate(company);
            Map<String, String> vars = new HashMap<>();
            vars.put("SUBJECT", "Thông báo mua gói hóa đơn thành công");
            vars.put("NAME", firstNotBlank(purchase.getBuyerName(), company.getContactName(), company.getName(), user.getUsername(), ""));
            vars.put("COMPANY", nullToBlank(company.getName()));
            vars.put("PACKAGE_NAME", nullToBlank(purchase.getPackageName()));
            vars.put("INVOICE_QUANTITY", String.valueOf(valueOrZero(purchase.getInvoiceQuantity())));
            vars.put("UNIT_PRICE", formatMoney(purchase.getUnitPrice()));
            vars.put("AMOUNT", formatMoney(purchase.getTotalPrice()));
            vars.put("TOTAL_PRICE", formatMoney(purchase.getTotalPrice()));
            vars.put("PAYMENT_METHOD", nullToBlank(purchase.getPaymentMethod()));
            vars.put("PAYMENT_CODE", nullToBlank(purchase.getPaymentCode()));
            vars.put("PAID_AT", purchase.getPaidAt() != null ? purchase.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
            vars.put("TOTAL_INVOICES", String.valueOf(valueOrZero(buyInvoice.getAmount())));
            vars.put("USED_INVOICES", String.valueOf(valueOrZero(buyInvoice.getAmountUsed())));
            vars.put("REMAINING_INVOICES", String.valueOf(valueOrZero(buyInvoice.getAmount()) - valueOrZero(buyInvoice.getAmountUsed())));
            vars.put("HTML_BODY", fallbackMailBody(vars));

            MailJobMessage message = new MailJobMessage();
            message.setTemplateKey(MAIL_KEY);
            message.setCompanyId(company.getId());
            message.setToEmail(toEmail);
            message.setToName(vars.get("NAME"));
            message.setVariables(vars);
            mailQueueService.enqueue(message);
        } catch (Exception e) {
            log.warn("Không thể đưa mail mua gói hóa đơn vào hàng đợi: {}", e.getMessage());
        }
    }

    private void ensureBuyInvoiceMailTemplate(CompanyEntity company) {
        MailTemplateEntity existing = mailTemplateRepository.findSystemByKey(MAIL_KEY);
        if (existing != null) {
            return;
        }
        CompanyEntity templateCompany = companyRepository.findById(SystemMail.COMPANY_ID)
                .orElse(company);
        MailTemplateEntity template = new MailTemplateEntity();
        template.setCompany(templateCompany);
        template.setKey(MAIL_KEY);
        template.setTitle("Thông báo mua gói hóa đơn thành công");
        template.setStatus((byte) 1);
        template.setSystem((byte) 1);
        template.setContent(defaultMailTemplate());
        mailTemplateRepository.save(template);
    }

    private String defaultMailTemplate() {
        return """
                <div style="background:#eeeeee;padding:24px 0;font-family:Roboto,Helvetica,Arial,sans-serif;color:#212b35">
                  <div style="background:#ffffff;margin:0 auto;max-width:600px;padding:28px">
                    <p style="font-size:16px;margin:0 0 16px">Xin chào [NAME],</p>
                    <p style="font-size:14px;line-height:24px;margin:0 0 18px">Hệ thống đã ghi nhận giao dịch mua gói hóa đơn của quý khách.</p>
                    <table style="width:100%;border-collapse:collapse;font-size:14px;line-height:22px">
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Công ty</td><td style="padding:8px;border-bottom:1px solid #edf2f7"><strong>[COMPANY]</strong></td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Gói hóa đơn</td><td style="padding:8px;border-bottom:1px solid #edf2f7">[PACKAGE_NAME]</td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Số hóa đơn</td><td style="padding:8px;border-bottom:1px solid #edf2f7">[INVOICE_QUANTITY]</td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Tổng thanh toán</td><td style="padding:8px;border-bottom:1px solid #edf2f7"><strong>[TOTAL_PRICE]</strong></td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Phương thức</td><td style="padding:8px;border-bottom:1px solid #edf2f7">[PAYMENT_METHOD]</td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #edf2f7">Mã giao dịch</td><td style="padding:8px;border-bottom:1px solid #edf2f7">[PAYMENT_CODE]</td></tr>
                    </table>
                    <p style="font-size:14px;line-height:24px;margin:18px 0 0">Số hóa đơn còn lại hiện tại: <strong>[REMAINING_INVOICES]</strong>.</p>
                    <p style="font-size:14px;line-height:24px;margin:18px 0 0">Đây là mail tự động từ hệ thống. Vui lòng không trả lời mail này.</p>
                    <p style="font-size:14px;margin:18px 0 0">Trân trọng!</p>
                  </div>
                  <div style="margin:0 auto;max-width:600px;padding:12px 28px;text-align:center;color:#555;font-size:12px">Giải Pháp Hóa Đơn Điện Tử Thông Minh được cung cấp bởi P.A Việt Nam</div>
                </div>
                """;
    }

    private String fallbackMailBody(Map<String, String> vars) {
        return "<p>Xin chào " + vars.get("NAME") + ",</p>"
                + "<p>Quý khách đã mua thành công gói " + vars.get("PACKAGE_NAME")
                + " với số lượng " + vars.get("INVOICE_QUANTITY") + " hóa đơn.</p>";
    }

    private void ensureDefaultPackage() {
        ensureBuyInvoiceMailTemplateIfPossible();
        if (packageRepository.count() > 0) {
            return;
        }
        InvoicePackageEntity sample = new InvoicePackageEntity();
        sample.setName("Bill #1");
        sample.setInvoiceQuantity(500);
        sample.setUnitPrice(BigDecimal.valueOf(600));
        sample.setTotalPrice(BigDecimal.valueOf(300000));
        sample.setIncludeTrial(true);
        sample.setDescription("Bao gồm gói trải nghiệm");
        sample.setStatus(1);
        sample.setDisplayOrder(1);
        packageRepository.save(sample);
    }

    private void ensureBuyInvoiceMailTemplateIfPossible() {
        if (mailTemplateRepository.findSystemByKey(MAIL_KEY) != null) {
            return;
        }
        List<CompanyEntity> companies = companyRepository.findAll(PageRequest.of(0, 1)).getContent();
        if (!companies.isEmpty()) {
            ensureBuyInvoiceMailTemplate(companies.get(0));
        }
    }

    private InvoicePackageResponseDTO toPackageDto(InvoicePackageEntity entity) {
        InvoicePackageResponseDTO dto = new InvoicePackageResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setInvoiceQuantity(entity.getInvoiceQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setIncludeTrial(entity.getIncludeTrial());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private InvoicePackagePurchaseDTO toPurchaseDto(InvoicePackagePurchaseEntity entity) {
        InvoicePackagePurchaseDTO dto = new InvoicePackagePurchaseDTO();
        dto.setId(entity.getId());
        dto.setPackageId(entity.getPackageId());
        dto.setPackageName(entity.getPackageName());
        dto.setCompanyId(entity.getCompanyId());
        dto.setCompanyName(entity.getCompanyName());
        dto.setCompanyTaxcode(entity.getCompanyTaxcode());
        dto.setBuyerName(entity.getBuyerName());
        dto.setBuyerEmail(entity.getBuyerEmail());
        dto.setBuyerPhone(entity.getBuyerPhone());
        dto.setInvoiceQuantity(entity.getInvoiceQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setPaymentStatus(entity.getPaymentStatus());
        dto.setPaymentCode(entity.getPaymentCode());
        dto.setBuyInvoiceId(entity.getBuyInvoiceId());
        dto.setPaidAt(entity.getPaidAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private InvoicePackagePurchaseFilterDTO normalizeStatsFilter(InvoicePackagePurchaseFilterDTO filter) {
        InvoicePackagePurchaseFilterDTO normalized = filter != null ? filter : new InvoicePackagePurchaseFilterDTO();
        if (normalized.getFromDate() == null && normalized.getToDate() == null) {
            LocalDate now = LocalDate.now();
            normalized.setFromDate(LocalDate.of(now.getYear(), 1, 1));
            normalized.setToDate(LocalDate.of(now.getYear(), 12, 31));
        }
        return normalized;
    }

    private Map<String, InvoicePackageMonthlyStatisticDTO> initMonthMap(InvoicePackagePurchaseFilterDTO filter) {
        LocalDate from = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now().withDayOfYear(1);
        LocalDate to = filter.getToDate() != null ? filter.getToDate() : LocalDate.now();
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        Map<String, InvoicePackageMonthlyStatisticDTO> map = new LinkedHashMap<>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            map.put(cursor.toString(), emptyMonth(cursor.toString()));
            cursor = cursor.plusMonths(1);
        }
        return map;
    }

    private InvoicePackageMonthlyStatisticDTO emptyMonth(String month) {
        InvoicePackageMonthlyStatisticDTO item = new InvoicePackageMonthlyStatisticDTO();
        item.setMonth(month);
        item.setOrderCount(0L);
        item.setInvoiceQuantity(0);
        item.setRevenue(BigDecimal.ZERO);
        return item;
    }

    private BigDecimal resolveTotalPrice(InvoicePackageRequestDTO dto) {
        if (dto.getTotalPrice() != null && dto.getTotalPrice().compareTo(BigDecimal.ZERO) >= 0) {
            return money(dto.getTotalPrice());
        }
        return money(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getInvoiceQuantity())));
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String method = paymentMethod != null ? paymentMethod.trim().toUpperCase(Locale.ROOT) : "";
        if (!"MOMO".equals(method) && !"VNPAY".equals(method)) {
            return "MOMO";
        }
        return method;
    }

    private String fakePaymentCode(String method) {
        return method + "-FAKE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return nf.format(value != null ? value : BigDecimal.ZERO);
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0D;
    }

    private String resolveBuyerName(UserEntity user, CompanyEntity company) {
        return firstNotBlank(user.getName(), company.getContactName(), company.getName(), user.getUsername());
    }

    private String resolveBuyerEmail(UserEntity user, CompanyEntity company) {
        return firstNotBlank(user.getEmail(), company.getContactMail(), company.getEmail());
    }

    private String resolveBuyerPhone(UserEntity user, CompanyEntity company) {
        return firstNotBlank(user.getPhone(), company.getContactPhone(), company.getHotline());
    }

    private String firstNotBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullToBlank(String value) {
        return value != null ? value : "";
    }
}
