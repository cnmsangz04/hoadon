package vn.hoadon.services.patvan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.hoadon.config.AppEnvironment;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.HistoryEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.SignatureAuthoritiesTaxEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.ErrorMessageRepository;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.HistoryRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;
import vn.hoadon.repositories.SignatureAuthoritiesTaxRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatvanCallbackServiceTest {
    private InvoiceRepository invoiceRepository;
    private RegisterInvoiceRepository registerInvoiceRepository;
    private ErrorMessageRepository errorMessageRepository;
    private CompanyRepository companyRepository;
    private FormInvoiceRepository formInvoiceRepository;
    private HistoryRepository historyRepository;
    private SignatureAuthoritiesTaxRepository signatureRepository;
    private PatvanCallbackService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        registerInvoiceRepository = mock(RegisterInvoiceRepository.class);
        errorMessageRepository = mock(ErrorMessageRepository.class);
        companyRepository = mock(CompanyRepository.class);
        formInvoiceRepository = mock(FormInvoiceRepository.class);
        historyRepository = mock(HistoryRepository.class);
        signatureRepository = mock(SignatureAuthoritiesTaxRepository.class);
        service = new PatvanCallbackService(invoiceRepository, registerInvoiceRepository,
                errorMessageRepository, companyRepository, formInvoiceRepository,
                historyRepository, signatureRepository,
                new AppEnvironment("https://hoadon2er.id.vn"), "0302431595");
    }

    @Test
    void sharedEnvironmentDetectsLocalAndProduction() {
        assertThat(new AppEnvironment("http://hoadon2er.local.vn").isLocal()).isTrue();
        assertThat(new AppEnvironment("http://localhost:8081").isLocal()).isTrue();
        assertThat(new AppEnvironment("http://127.0.0.1:8081").isLocal()).isTrue();
        assertThat(new AppEnvironment("https://hoadon2er.id.vn").isProduction()).isTrue();
        assertThat(new AppEnvironment("://invalid").isProduction()).isTrue();
    }

    @Test
    void message202UpdatesInvoiceAndStoresTaxAuthorityXml() {
        String reference = "PATVAN-202-001";
        InvoiceEntity invoice = invoice(reference);
        stubReference(reference, invoice);
        when(signatureRepository.findTopByInvoiceIdAndCompanyIdOrderByIdDesc(99, 10))
                .thenReturn(Optional.empty());

        String xml = """
                <TDiep>
                  <TTChung><MLTDiep>202</MLTDiep><MTDTChieu>PATVAN-202-001</MTDTChieu></TTChung>
                  <DLieu><HDon><DLHDon/><MCCQT>C012345678901234567890123456789012</MCCQT></HDon></DLieu>
                </TDiep>
                """;

        PatvanCallbackService.Result result = service.process("0302431595", "0101234567", xml);

        assertThat(result.status()).isEqualTo(200);
        assertThat(invoice.getStatus()).isEqualTo((short) 3);
        assertThat(invoice.getCodeCqt()).isEqualTo("C012345678901234567890123456789012");

        ArgumentCaptor<SignatureAuthoritiesTaxEntity> signatureCaptor =
                ArgumentCaptor.forClass(SignatureAuthoritiesTaxEntity.class);
        verify(signatureRepository).save(signatureCaptor.capture());
        assertThat(signatureCaptor.getValue().getXml()).contains("<HDon>").contains("<MCCQT>");

        ArgumentCaptor<HistoryEntity> historyCaptor = ArgumentCaptor.forClass(HistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getType()).isEqualTo(202);
        assertThat(historyCaptor.getValue().getDescription())
                .isEqualTo("C012345678901234567890123456789012");
    }

    @Test
    void message204AcceptsNoCodeInvoice() {
        String reference = "PATVAN-204-001";
        InvoiceEntity invoice = invoice(reference);
        stubReference(reference, invoice);
        when(errorMessageRepository.findFirstByMessageCodeAndCompanyIdOrderByIdDesc(reference, 10L))
                .thenReturn(Optional.empty());
        FormInvoiceEntity form = new FormInvoiceEntity();
        form.setHaveCode(0);
        when(formInvoiceRepository.findById(15L)).thenReturn(Optional.of(form));

        String xml = """
                <TDiep>
                  <TTChung><MLTDiep>204</MLTDiep><MTDTChieu>PATVAN-204-001</MTDTChieu></TTChung>
                  <DLieu><TBao><DLTBao><LTBao>2</LTBao><Ten>Đã tiếp nhận</Ten>
                    <CCu>Hóa đơn hợp lệ</CCu></DLTBao></TBao></DLieu>
                </TDiep>
                """;

        service.process("0302431595", "0101234567", xml);

        assertThat(invoice.getStatus()).isEqualTo((short) 3);
        assertThat(invoice.getCodeCqt()).isNull();
        ArgumentCaptor<HistoryEntity> historyCaptor = ArgumentCaptor.forClass(HistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getType()).isEqualTo(204);
        assertThat(historyCaptor.getValue().getDescription()).isEqualTo("Hóa đơn hợp lệ");
    }

    @Test
    void message204RejectsCodedInvoiceWhenLCMaExists() {
        String reference = "PATVAN-204-ERROR";
        InvoiceEntity invoice = invoice(reference);
        invoice.setCodeCqt("OLD-CODE");
        stubReference(reference, invoice);
        when(errorMessageRepository.findFirstByMessageCodeAndCompanyIdOrderByIdDesc(reference, 10L))
                .thenReturn(Optional.empty());
        FormInvoiceEntity form = new FormInvoiceEntity();
        form.setHaveCode(1);
        when(formInvoiceRepository.findById(15L)).thenReturn(Optional.of(form));

        String xml = """
                <TDiep>
                  <TTChung><MLTDiep>204</MLTDiep><MTDTChieu>PATVAN-204-ERROR</MTDTChieu></TTChung>
                  <DLieu><TBao><DLTBao><Ten>Từ chối cấp mã</Ten>
                    <LCMa><DSLDo>Sai dữ liệu người bán</DSLDo></LCMa>
                  </DLTBao></TBao></DLieu>
                </TDiep>
                """;

        service.process("0302431595", "0101234567", xml);

        assertThat(invoice.getStatus()).isEqualTo((short) 7);
        assertThat(invoice.getCodeCqt()).isNull();
        ArgumentCaptor<HistoryEntity> historyCaptor = ArgumentCaptor.forClass(HistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getDescription()).isEqualTo("Sai dữ liệu người bán");
    }

    private void stubReference(String reference, InvoiceEntity invoice) {
        CompanyEntity company = new CompanyEntity();
        company.setId(10L);
        company.setTaxcode("0101234567");
        company.setStatus(1);
        when(invoiceRepository.findByCode(reference)).thenReturn(List.of(invoice));
        when(registerInvoiceRepository.findByDeclarationCode(reference)).thenReturn(List.of());
        when(errorMessageRepository.findByMessageCode(reference)).thenReturn(List.of());
        when(companyRepository.findByTaxcode("0101234567")).thenReturn(Optional.of(company));
        when(invoiceRepository.findFirstByCodeAndCompanyIdOrderByIdDesc(reference, 10))
                .thenReturn(Optional.of(invoice));
        when(historyRepository.findFirstByCompanyIdAndTableNameAndTableIdAndTypeOrderByIdDesc(
                any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    private static InvoiceEntity invoice(String reference) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(99L);
        invoice.setCompanyId(10);
        invoice.setUserId(20);
        invoice.setFormId(15);
        invoice.setCode(reference);
        invoice.setStatus((short) 2);
        return invoice;
    }
}
