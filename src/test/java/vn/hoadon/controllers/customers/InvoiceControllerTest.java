package vn.hoadon.controllers.customers;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceControllerTest {

    @Test
    void invoiceMailGreetingNameUsesCustomerCompanyBeforeBuyer() {
        Map<String, Object> customer = Map.of(
                "name", "Cong ty ABC",
                "buyer", "Nguyen Van A"
        );

        assertThat(InvoiceController.invoiceMailGreetingName(customer)).isEqualTo("Cong ty ABC");
        assertThat(InvoiceController.invoiceCustomerBuyerName(customer)).isEqualTo("Nguyen Van A");
    }

    @Test
    void invoiceMailGreetingNameFallsBackToBuyerWhenCompanyIsBlank() {
        Map<String, Object> customer = Map.of(
                "name", " ",
                "buyer", "Nguyen Van A"
        );

        assertThat(InvoiceController.invoiceMailGreetingName(customer)).isEqualTo("Nguyen Van A");
    }
}
