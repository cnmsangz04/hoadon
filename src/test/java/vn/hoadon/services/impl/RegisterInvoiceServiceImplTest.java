package vn.hoadon.services.impl;

import org.junit.jupiter.api.Test;
import vn.hoadon.entity.ProvinceEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.LegalRepresentativeRepository;
import vn.hoadon.repositories.ProvinceRepository;
import vn.hoadon.repositories.RegisterInvoiceRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegisterInvoiceServiceImplTest {

    @Test
    void buildUnsignedXmlResolvesNumericCreatePlaceToProvinceName() {
        RegisterInvoiceRepository registerInvoiceRepository = mock(RegisterInvoiceRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        LegalRepresentativeRepository legalRepresentativeRepository = mock(LegalRepresentativeRepository.class);
        ProvinceRepository provinceRepository = mock(ProvinceRepository.class);

        ProvinceEntity province = new ProvinceEntity();
        province.setName("Hồ Chí Minh");
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());
        when(legalRepresentativeRepository.findByCompanyId(1L)).thenReturn(Optional.empty());
        when(provinceRepository.findById(2)).thenReturn(Optional.of(province));

        RegisterInvoiceServiceImpl service = new RegisterInvoiceServiceImpl(
                registerInvoiceRepository,
                companyRepository,
                legalRepresentativeRepository,
                provinceRepository
        );

        RegisterInvoiceEntity registerInvoice = new RegisterInvoiceEntity();
        registerInvoice.setCompanyId(1L);
        registerInvoice.setCreatePlace("2");
        registerInvoice.setDeclarationDate(LocalDate.of(2026, 6, 19));

        String xml = service.buildUnsignedXml(registerInvoice);

        assertThat(xml).contains("<DDanh>Hồ Chí Minh</DDanh>");
        assertThat(xml).doesNotContain("<DDanh>2</DDanh>");
    }
}
