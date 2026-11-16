package vn.hoadon.invoice.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.compiler.*;
import vn.hoadon.invoice.template.service.TemplateException;
import vn.hoadon.entity.InvoiceEntity;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class InvoiceLayoutTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private InvoiceFieldCatalog catalog;
    private JsonLayoutCompiler compiler;

    @BeforeEach
    void setUp() throws Exception {
        catalog = TestInvoiceFieldCatalog.create(mapper);
        compiler = new JsonLayoutCompiler(catalog);
    }

    @Test
    void catalogPreservesAllFieldsAndRequirements() {
        assertThat(catalog.latest().fields()).hasSize(98);
        assertThat(catalog.latest().requireField("items.special.DLieu").repeated()).isTrue();
        assertThat(catalog.latest().requireField("common.MSTDVNUNLHDon").description()).contains("ủy nhiệm");
        assertThat(catalog.latest().requireField("totals.TgTTTBSo").sourcePath()).isEqualTo("DLHDon/NDHDon/TToan/TgTTTBSo");
        assertThat(catalog.latest().requireField("invoice.codeCqt").sourceType()).isEqualTo("CONTEXT");
        assertThat(catalog.latest().requireField("invoice.lookupCode").sourcePath()).isEqualTo("invoice.lookup_code");
    }

    @Test
    void mapperKeepsNamespacesRowsAndNestedSpecialFields() {
        String xml = """
                <HDon xmlns="urn:invoice"><DLHDon><TTChung><SHDon>00000012</SHDon></TTChung><NDHDon><NBan><Ten>Đặng Thị Hà</Ten></NBan>
                <DSHHDVu><HHDVu><THHDVu>Mặt hàng thứ nhất</THHDVu><TTHHDTrung><TTin><TTruong>SoKhung</TTruong><DLieu>ABC</DLieu></TTin></TTHHDTrung></HHDVu>
                <HHDVu><THHDVu>Mặt hàng thứ hai</THHDVu></HHDVu></DSHHDVu><TToan><TgTTTBSo>1234567890123.456789</TgTTTBSo></TToan></NDHDon></DLHDon></HDon>
                """;
        var data = new InvoiceViewModelMapper(catalog).map(xml,
                Map.of("code_cqt", "MCCQT123", "lookup_code", "TRA-CUU-123"));
        assertThat(data.get("common.SHDon")).isEqualTo("00000012");
        assertThat(data.get("totals.TgTTTBSo").toString()).isEqualTo("1234567890123.456789");
        assertThat(data.get("invoice.codeCqt")).isEqualTo("MCCQT123");
        assertThat(data.get("invoice.lookupCode")).isEqualTo("TRA-CUU-123");
        @SuppressWarnings("unchecked") var rows = (List<Map<String, Object>>) data.get("items");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("items.THHDVu")).isEqualTo("Mặt hàng thứ nhất");
        assertThat(rows.get(1).get("items.THHDVu")).isEqualTo("Mặt hàng thứ hai");
        assertThat((List<?>) rows.get(0).get("items.special")).hasSize(1);
        assertThat((List<?>) rows.get(1).get("items.special")).isEmpty();
    }

    @Test
    void externalRegistryReadsMappedInvoiceColumnsOnly() {
        var registry = new InvoiceExternalFieldRegistry();
        var invoice = new InvoiceEntity();
        invoice.setCodeCqt("MCCQT456");
        invoice.setLookupCode("TRA-CUU-456");
        assertThat(InvoiceExternalFieldRegistry.contextSupports("invoice.code_cqt")).isTrue();
        assertThat(InvoiceExternalFieldRegistry.contextSupports("invoice.lookup_code")).isTrue();
        assertThat(InvoiceExternalFieldRegistry.contextSupports("invoice.issue_profile_snapshot_json")).isFalse();
        var context = registry.capture(invoice, TestInvoiceFieldCatalog.snapshot(new ObjectMapper()));
        assertThat(context).containsEntry("invoice.code_cqt", "MCCQT456").containsEntry("invoice.lookup_code", "TRA-CUU-456");
    }

    @Test
    void xmlExternalEntitiesAreRejected() {
        assertThatThrownBy(() -> InvoiceViewModelMapper.parse("<!DOCTYPE HDon [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><HDon>&x;</HDon>"))
                .isInstanceOf(TemplateException.class).hasMessageContaining("XML");
    }

    @Test
    void defaultLayoutCompilesRealInvoiceFixtureWithoutLosingValues() throws Exception {
        var layout = defaultLayout();
        compiler.validate(layout, true);
        String xml = new InvoiceFixtureFactory(mapper).sample(1, 3);
        String html = compiler.compile(layout, new InvoiceViewModelMapper(catalog).map(xml, Map.of("sampleMarker", "SAMPLE")));
        assertThat(html).contains("HÓA ĐƠN GIÁ TRỊ GIA TĂNG", "Hàng hóa, dịch vụ minh họa 3", "330.000", "<thead>", "Mẫu — Dữ liệu minh họa");
    }

    @Test
    void arbitraryCssAndUnknownBindingsCannotReachRenderer() throws Exception {
        var layout = defaultLayout();
        ((ObjectNode) layout.path("body").path("children").get(0).path("style")).put("position", "absolute");
        var invalidStyle = layout;
        assertThatThrownBy(() -> compiler.validate(invalidStyle, true)).isInstanceOf(TemplateException.class).hasMessageContaining("position");
        layout = defaultLayout();
        ObjectNode field = (ObjectNode) layout.findParents("fieldKey").stream()
                .filter(node -> "field".equals(node.path("type").asText()))
                .findFirst().orElseThrow();
        field.put("fieldKey", "seller.Secret");
        var invalid = layout;
        assertThatThrownBy(() -> compiler.validate(invalid, true)).isInstanceOf(TemplateException.class).hasMessageContaining("seller.Secret");
    }

    @Test
    void publishedLayoutCannotHideRequiredFieldBehindCondition() throws Exception {
        var layout = defaultLayout();
        ((ObjectNode) layout.path("body")).set("condition", mapper.readTree("{\"op\":\"equals\",\"fieldKey\":\"seller.MST\",\"value\":\"never\"}"));
        assertThatThrownBy(() -> compiler.validate(layout, true)).isInstanceOf(TemplateException.class).hasMessageContaining("thiếu trường");
    }

    private com.fasterxml.jackson.databind.JsonNode defaultLayout() throws Exception {
        try (var input = getClass().getResourceAsStream("/invoice-template/default-layout.json")) { return mapper.readTree(input); }
    }
}
