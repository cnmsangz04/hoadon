package vn.hoadon.util;

import org.junit.jupiter.api.Test;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceXmlBuilderTest {

    @Test
    void registerInvoiceXmlUsesOfficialTagsAndDateTimeForCertificates() {
        RegisterInvoiceEntity registerInvoice = new RegisterInvoiceEntity();
        registerInvoice.setInvoiceTypes(List.of("HDGTGT_BienLai", "HDBHang_BienLai", "HDThuongMai"));
        registerInvoice.setDigitalCertificates(List.of(
                "{\"orgName\":\"i-ca corp\",\"serialNo\":\"SERIAL\",\"signFromDate\":\"2026-10-01\",\"signToDate\":\"2027-10-27\",\"sigRegMethod\":1}"
        ));

        String xml = RegisterInvoiceXmlBuilder.buildUnsigned(registerInvoice);

        assertThat(xml).contains("<HDGTGTTHBLai>1</HDGTGTTHBLai>");
        assertThat(xml).contains("<HDBHTHBLai>1</HDBHTHBLai>");
        assertThat(xml).contains("<HDTMai>1</HDTMai>");
        assertThat(xml).contains("<TNgay>2026-10-01T00:00:00</TNgay>");
        assertThat(xml).contains("<DNgay>2027-10-27T00:00:00</DNgay>");
        assertThat(xml).doesNotContain("HDGTGT_BienLai", "HDBHang_BienLai", "HDThuongMai");
        assertThat(xml).doesNotContain("<TTTCGP>", "<TTTCTN>");
    }

    @Test
    void registerInvoiceXmlRendersConfiguredProviderLists() {
        RegisterInvoiceEntity registerInvoice = new RegisterInvoiceEntity();
        registerInvoice.setSolutionProviders(List.of(
                "{\"organName\":\"Công ty cung cấp giải pháp\",\"organTaxCode\":\"0100000000\",\"organFromDate\":\"2021-02-14\"}"
        ));
        registerInvoice.setTransmitProviders(List.of(
                "{\"organName\":\"Công ty truyền nhận\",\"organTaxCode\":\"0200000000\",\"organFromDate\":\"2022-02-14\"}"
        ));

        String xml = RegisterInvoiceXmlBuilder.buildUnsigned(registerInvoice);

        assertThat(xml).contains("<TTTCGP><TCGP><STT>1</STT><TTCGP>Công ty cung cấp giải pháp</TTCGP><MSTTCGP>0100000000</MSTTCGP><TNgay>2021-02-14</TNgay>");
        assertThat(xml).contains("<TTTCTN><TCTN><STT>1</STT><TTCTN>Công ty truyền nhận</TTCTN><MSTTCTN>0200000000</MSTTCTN><TNgay>2022-02-14</TNgay>");
    }

    @Test
    void registerInvoiceXmlUsesResolvedCreatePlaceNameForDeclarationPlace() {
        RegisterInvoiceEntity registerInvoice = new RegisterInvoiceEntity();
        registerInvoice.setCreatePlace("2");
        registerInvoice.setDeclarationDate(LocalDate.of(2026, 6, 19));

        String xml = RegisterInvoiceXmlBuilder.buildUnsigned(
                registerInvoice,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Cong ty test",
                "0100000000",
                "Hồ Chí Minh"
        );

        assertThat(xml).contains("<DDanh>Hồ Chí Minh</DDanh>");
        assertThat(xml).doesNotContain("<DDanh>2</DDanh>");
    }

    @Test
    void registerInvoiceXmlUsesProvidedSignerDocumentId() {
        RegisterInvoiceEntity registerInvoice = new RegisterInvoiceEntity();
        registerInvoice.setDeclarationDate(LocalDate.of(2026, 10, 2));

        String xml = RegisterInvoiceXmlBuilder.buildUnsigned(
                registerInvoice,
                null, null, null, null, null, null, null, null,
                null, null, "Cong ty test", "0100000000", "Hồ Chí Minh",
                "SIGN_TARGET_123"
        );

        assertThat(xml).contains("<DLTKhai Id=\"SIGN_TARGET_123\">");
    }

    @Test
    void sampleInvoiceXmlDefaultsFinancialLeaseFlagToZero() {
        String xml = SampleInvoiceXmlBuilder.build();

        assertThat(xml).contains("<HDCTTChinh>0</HDCTTChinh>");
        assertThat(xml.indexOf("<NLap>")).isLessThan(xml.indexOf("<HDCTTChinh>0</HDCTTChinh>"));
        assertThat(xml.indexOf("<HDCTTChinh>0</HDCTTChinh>")).isLessThan(xml.indexOf("<SBKe>"));
    }

    @Test
    void invoiceXmlDefaultsFinancialLeaseFlagToZero() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setName("Hoa don GTGT");
        invoice.setNo(1);
        invoice.setDateExport(LocalDate.of(2026, 6, 17));
        invoice.setCurrency("VND");
        invoice.setExchangeRate(1D);
        invoice.setPaymentType((short) 1);
        invoice.setCustomer("{}");
        invoice.setDetail("[]");
        invoice.setTotal(0D);
        invoice.setVatAmount(0D);
        invoice.setDiscountAmount(0D);
        invoice.setAmount(0D);
        invoice.setAmountInWords("Khong dong");

        FormInvoiceEntity form = new FormInvoiceEntity();
        form.setFormCode("1");
        form.setSerial("C26TAA");

        CompanyEntity company = new CompanyEntity();
        company.setName("Cong ty test");
        company.setTaxcode("0100000000");
        company.setAddress("Ha Noi");

        String xml = InvoiceXmlBuilder.build(invoice, form, company, null);

        assertThat(xml).contains("<HDCTTChinh>0</HDCTTChinh>");
        assertThat(xml.indexOf("<NLap>")).isLessThan(xml.indexOf("<HDCTTChinh>0</HDCTTChinh>"));
        assertThat(xml.indexOf("<HDCTTChinh>0</HDCTTChinh>")).isLessThan(xml.indexOf("<SBKe>"));
    }

    @Test
    void relatedInvoiceXmlContainsAdjustmentReplacementReason() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setName("Hoa don GTGT");
        invoice.setNo(2);
        invoice.setDateExport(LocalDate.of(2026, 10, 22));
        invoice.setCurrency("VND");
        invoice.setExchangeRate(1D);
        invoice.setPaymentType((short) 1);
        invoice.setCustomer("{}");
        invoice.setDetail("[]");
        invoice.setTotal(0D);
        invoice.setVatAmount(0D);
        invoice.setDiscountAmount(0D);
        invoice.setAmount(0D);
        invoice.setAmountInWords("Khong dong");
        invoice.setInvoiceType((short) 2);
        invoice.setRelated("4");

        InvoiceEntity original = new InvoiceEntity();
        original.setNo(1);
        original.setDateExport(LocalDate.of(2026, 10, 20));

        FormInvoiceEntity form = new FormInvoiceEntity();
        form.setFormCode("1");
        form.setSerial("C26TAA");

        FormInvoiceEntity originalForm = new FormInvoiceEntity();
        originalForm.setFormCode("1");
        originalForm.setSerial("C26TAA");
        originalForm.setCategory(1);

        String xml = InvoiceXmlBuilder.build(invoice, form, null, null, original, originalForm);

        assertThat(xml).contains("<TCHDon>2</TCHDon><LDDCTThe>4</LDDCTThe>");
    }

    @Test
    void oneVatRateFormUsesSingleVatRateSummaryWhenInvoiceVatRateIsMissing() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setName("Hoa don GTGT");
        invoice.setNo(2);
        invoice.setDateExport(LocalDate.of(2026, 6, 17));
        invoice.setCurrency("VND");
        invoice.setExchangeRate(1D);
        invoice.setPaymentType((short) 1);
        invoice.setCustomer("{}");
        invoice.setDetail("""
                [
                  {"num":1,"name":"Hang hoa A","quantity":1,"price":100000,"total":100000,"vatRate":8,"vatAmount":8000,"amount":108000},
                  {"num":2,"name":"Hang hoa B","quantity":1,"price":50000,"total":50000,"vatRate":8,"vatAmount":4000,"amount":54000}
                ]
                """);
        invoice.setTotal(150000D);
        invoice.setVatAmount(12000D);
        invoice.setDiscountAmount(0D);
        invoice.setAmount(162000D);
        invoice.setAmountInWords("Mot tram sau muoi hai nghin dong");
        invoice.setVatRate((short) -1);

        FormInvoiceEntity form = new FormInvoiceEntity();
        form.setFormCode("1");
        form.setSerial("C26TAA");
        form.setType(1);

        CompanyEntity company = new CompanyEntity();
        company.setName("Cong ty test");
        company.setTaxcode("0100000000");
        company.setAddress("Ha Noi");

        String xml = InvoiceXmlBuilder.build(invoice, form, company, null);

        assertThat(xml).contains("<TSuat>8%</TSuat>");
        assertThat(xml).contains("<LTSuat><TSuat>8%</TSuat><ThTien>150000</ThTien><TThue>12000</TThue></LTSuat>");
        assertThat(count(xml, "<LTSuat>")).isEqualTo(1);
    }

    @Test
    void oldXmlMissingFinancialLeaseFlagIsNormalizedWithZero() {
        String xml = "<HDon><DLHDon><TTChung>"
                + "<NLap>2026-06-17</NLap>"
                + "<SBKe></SBKe>"
                + "</TTChung></DLHDon></HDon>";

        String normalized = InvoiceXmlTaxValidator.addDefaultValues(xml);

        assertThat(normalized).contains("<HDCTTChinh>0</HDCTTChinh>");
        assertThat(normalized.indexOf("</NLap>")).isLessThan(normalized.indexOf("<HDCTTChinh>0</HDCTTChinh>"));
        assertThat(normalized.indexOf("<HDCTTChinh>0</HDCTTChinh>")).isLessThan(normalized.indexOf("<SBKe>"));
    }

    @Test
    void existingFinancialLeaseFlagIsKept() {
        String xml = "<HDon><DLHDon><TTChung>"
                + "<NLap>2026-06-17</NLap>"
                + "<HDCTTChinh>1</HDCTTChinh>"
                + "<SBKe></SBKe>"
                + "</TTChung></DLHDon></HDon>";

        String normalized = InvoiceXmlTaxValidator.addDefaultValues(xml);

        assertThat(normalized).isEqualTo(xml);
    }

    private static int count(String text, String token) {
        int total = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            total++;
            index += token.length();
        }
        return total;
    }
}
