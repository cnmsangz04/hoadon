package vn.hoadon.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageXsltRendererTest {

    private static final String XML = """
            <TBao>
              <DLTBao Id="TEST">
                <PBan>2.1.1</PBan>
                <MSo>04/SS-HĐĐT</MSo>
                <Ten>Thông báo hóa đơn điện tử có sai sót</Ten>
                <Loai>1</Loai><So/><NTBCCQT/>
                <TCQT>Tổng cục Thuế UAT</TCQT><MCQT>99999</MCQT>
                <MST>0302431595-999</MST><TNNT>Công ty TNHH P.A Việt Nam TEST</TNNT>
                <DDanh>Hồ Chí Minh</DDanh><NTBao>2026-10-02</NTBao>
                <DSHDon><HDon><STT>1</STT><MCCQT>ABC123</MCCQT><KHMSHDon>1</KHMSHDon>
                  <KHHDon>C26THD</KHHDon><SHDon>17</SHDon><Ngay>2026-10-02</Ngay>
                  <LADHDDT>1</LADHDDT><LDo>Sai tên người mua</LDo></HDon></DSHDon>
              </DLTBao>
              <DSCKS><NNT>
                <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
                  <Object><SignatureProperties><SignatureProperty>
                    <SigningTime>2026-10-02T10:20:21</SigningTime>
                  </SignatureProperty></SignatureProperties></Object>
                </Signature>
              </NNT></DSCKS>
            </TBao>
            """;

    @Test
    void rendersErrorMessageXmlWithRuntimeXslt() {
        String html = ErrorMessageXsltRenderer.render(XML, false);

        assertThat(html)
                .contains("Thông báo hóa đơn điện tử")
                .contains("Công ty TNHH P.A Việt Nam TEST")
                .contains("0302431595-999")
                .contains("C26THD")
                .contains("Sai tên người mua");
    }

    @Test
    void renderedHtmlCanBeConvertedToPdf() throws Exception {
        String html = ErrorMessageXsltRenderer.render(XML, false);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            assertThat(output.size()).isGreaterThan(1000);
        }
    }

    @Test
    void rendersSigningDateFromXmlDsigNamespace() {
        String html = ErrorMessageXsltRenderer.render(XML, true);

        assertThat(html).containsPattern("Ký ngày:\\s*<span>\\s*02 /\\s*10 /\\s*2026");
    }
}
