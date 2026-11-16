package vn.hoadon.invoice.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.hoadon.entity.*;
import vn.hoadon.invoice.template.persistence.CompanyInvoiceDefaultRepository;
import vn.hoadon.repositories.*;
import vn.hoadon.services.impl.FormInvoiceServiceImpl;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FormInvoiceWorkflowTest {
    private final FormInvoiceRepository forms = mock(FormInvoiceRepository.class);
    private final InvoiceNumberRepository counters = mock(InvoiceNumberRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final RegisterInvoiceRepository declarations = mock(RegisterInvoiceRepository.class);
    private final CompanyInvoiceDefaultRepository defaults = mock(CompanyInvoiceDefaultRepository.class);
    private FormInvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FormInvoiceServiceImpl(forms, counters);
        ReflectionTestUtils.setField(service, "companies", companies);
        ReflectionTestUtils.setField(service, "invoices", invoices);
        ReflectionTestUtils.setField(service, "declarations", declarations);
        ReflectionTestUtils.setField(service, "defaults", defaults);
        ReflectionTestUtils.setField(service, "formTemplates", mock(vn.hoadon.invoice.template.service.InvoiceFormTemplateService.class));
        ReflectionTestUtils.setField(service, "entityManager", mock(jakarta.persistence.EntityManager.class));
    }

    @Test
    void activatingSystemTemplateNeverCreatesOrMovesInvoiceCounter() {
        var form = form(0, 1);
        when(forms.saveAndFlush(form)).thenReturn(form);
        service.create(form);
        verifyNoInteractions(counters, companies, declarations);
    }

    @Test
    void inactiveFormDoesNotCreateAnEmptyDefault() {
        var form = form(1, 0);
        when(forms.saveAndFlush(form)).thenReturn(form);
        when(companies.lockById(1L)).thenReturn(Optional.of(new CompanyEntity()));
        service.create(form);
        verify(defaults, never()).save(any());
        verifyNoInteractions(counters);
    }

    @Test
    void haveCodeChangesAreSavedAndValidatedAgainstSerial() {
        var form = form(1, 0);
        when(forms.findById(10L)).thenReturn(Optional.of(form));
        when(companies.lockById(1L)).thenReturn(Optional.of(new CompanyEntity()));
        when(forms.save(form)).thenReturn(form);
        var patch = new FormInvoiceEntity();
        patch.setSerial("K26TAA");
        patch.setHaveCode(0);
        var saved = service.update(10L, patch).orElseThrow();
        assertThat(saved.getHaveCode()).isZero();
        assertThat(saved.getSerial()).isEqualTo("K26TAA");
        verifyNoInteractions(counters);
    }

    @Test
    void usedFiscalConfigurationCannotBeMutated() {
        var form = form(1, 0);
        when(forms.findById(10L)).thenReturn(Optional.of(form));
        when(companies.lockById(1L)).thenReturn(Optional.of(new CompanyEntity()));
        when(invoices.existsByFormId(10)).thenReturn(true);
        var patch = new FormInvoiceEntity();
        patch.setSerial("K26TAA");
        patch.setHaveCode(0);
        assertThatThrownBy(() -> service.update(10L, patch)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("đã được sử dụng");
        assertThat(form.getSerial()).isEqualTo("C26TAA");
    }

    @Test
    void activationKeepsExistingCounterUntilReconciliation() {
        var form = form(1, 1);
        var declaration = new RegisterInvoiceEntity();
        declaration.setInvoiceTypes(List.of("HDGTGT"));
        declaration.setInvoiceForms(List.of("CMa"));
        when(declarations.findLatestAccepted(eq(1L), any())).thenReturn(List.of(declaration));
        when(companies.lockById(1L)).thenReturn(Optional.of(new CompanyEntity()));
        when(forms.saveAndFlush(form)).thenReturn(form);
        var counter = new InvoiceNumberEntity();
        counter.setId(55L);
        counter.setTotal(128);
        when(counters.findByCompanyIdAndCategoryAndStatus(1L, 1, 1)).thenReturn(List.of(counter));
        service.create(form);
        assertThat(counter.getId()).isEqualTo(55L);
        assertThat(counter.getTotal()).isEqualTo(128);
        verify(forms).deactivateOthers(1L, 1, 10L);
    }

    @Test
    void deleteChecksCounterReferencesBeforeDeletingRecord() {
        var form = form(1, 0);
        when(forms.findById(10L)).thenReturn(Optional.of(form));
        when(companies.lockById(1L)).thenReturn(Optional.of(new CompanyEntity()));
        when(counters.findByCompanyIdAndFormId(1L, 10L)).thenReturn(List.of(new InvoiceNumberEntity()));
        assertThatThrownBy(() -> service.delete(10L)).isInstanceOf(IllegalArgumentException.class);
        verify(forms, never()).delete(any(FormInvoiceEntity.class));
    }

    @Test
    void inconsistentFormCanStillBeDeactivated() {
        var form = form(1, 1);
        form.setHaveCode(0);
        when(forms.findById(10L)).thenReturn(Optional.of(form));
        when(companies.lockById(1L)).thenReturn(Optional.of(new CompanyEntity()));
        when(forms.save(form)).thenReturn(form);
        var patch = new FormInvoiceEntity();
        patch.setStatus(0);
        assertThat(service.update(10L, patch).orElseThrow().getStatus()).isZero();
        verifyNoInteractions(counters);
    }

    private FormInvoiceEntity form(int system, int status) {
        var form = new FormInvoiceEntity();
        form.setId(10L);
        form.setCompanyId(1L);
        form.setSystem(system);
        form.setStatus(status);
        form.setName("Mẫu hóa đơn");
        form.setCategory(1);
        form.setType(1);
        form.setHaveCode(1);
        form.setTemplateVersionId(1L);
        form.setFormCode(system == 1 ? "1" : "X");
        form.setSerial(system == 1 ? "C26TAA" : "XXXXXX");
        return form;
    }
}
