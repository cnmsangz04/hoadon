package vn.hoadon.util;

import org.junit.jupiter.api.Test;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceXmlBuilderTest {

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
}
