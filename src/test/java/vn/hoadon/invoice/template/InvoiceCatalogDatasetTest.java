package vn.hoadon.invoice.template;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.compiler.JsonLayoutCompiler;

import java.util.*;

class InvoiceCatalogDatasetTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void keepsSingleChildFieldsInsideTheirParentRows() throws Exception {
        var source = detailSnapshot(false);
        var catalog = new InvoiceFieldCatalog(source);
        assertThat(catalog.latest().requireField("items.detail.Code").repeated()).isTrue();
        assertThat(source.rowScope("items.detail")).isEqualTo("items");
        var data =
                new InvoiceViewModelMapper(catalog)
                        .map(
                                "<HDon><DLHDon><NDHDon><DSHHDVu>"
                                        + "<HHDVu><Detail><Code>ONE</Code></Detail></HHDVu>"
                                        + "<HHDVu><Detail><Code>TWO</Code></Detail></HHDVu>"
                                        + "</DSHHDVu></NDHDon></DLHDon></HDon>",
                                Map.of());
        var layout = tableLayout("items", "items.detail.Code");
        var compiler = new JsonLayoutCompiler(catalog);
        assertThat(compiler.compile(layout, data)).contains("ONE", "TWO");
        ((com.fasterxml.jackson.databind.node.ObjectNode) layout.path("body"))
                .putArray("children")
                .addObject()
                .put("id", "outside")
                .put("type", "field")
                .put("fieldKey", "items.detail.Code");
        assertThatThrownBy(() -> compiler.validate(layout, false))
                .hasMessageContaining("Trường lặp");
    }

    @Test
    void rejectsSystemValuesInsideSingleChildrenOfRepeatingRows() {
        assertThatThrownBy(() -> InvoiceCatalogValidator.validate(detailSnapshot(true)))
                .hasMessageContaining("thuộc danh sách lặp");
    }

    @Test
    void rejectsNestedListsAsTopLevelTables() throws Exception {
        var source = TestInvoiceFieldCatalog.snapshot(mapper);
        var field =
                source.fields().stream()
                        .filter(f -> f.dataset().equals("items.special"))
                        .findFirst()
                        .orElseThrow();
        var layout = tableLayout("items.special", field.key());
        assertThatThrownBy(
                        () ->
                                new JsonLayoutCompiler(new InvoiceFieldCatalog(source))
                                        .validate(layout, false))
                .hasMessageContaining("cấp hóa đơn");
    }

    private InvoiceCatalogSnapshot detailSnapshot(boolean context) {
        var source = TestInvoiceFieldCatalog.snapshot(mapper);
        var datasets = new ArrayList<>(source.datasets());
        datasets.add(
                new InvoiceCatalogSnapshot.Dataset(
                        2000L,
                        "items.detail",
                        "Chi tiết hàng",
                        "items",
                        "ONE",
                        "Detail",
                        true,
                        0L));
        var fields = new ArrayList<>(source.fields());
        fields.add(
                new InvoiceCatalogSnapshot.Field(
                        2000L,
                        "items.detail.Code",
                        "Mã chi tiết",
                        "common",
                        "items.detail",
                        false,
                        context ? "CONTEXT" : "XML",
                        context ? "invoice.lookup_code" : "Code",
                        "STRING",
                        100,
                        "plainText",
                        "",
                        false,
                        true,
                        2000,
                        0L));
        return new InvoiceCatalogSnapshot(source.code(), source.groups(), datasets, fields);
    }

    private com.fasterxml.jackson.databind.JsonNode tableLayout(String dataset, String key)
            throws Exception {
        var layout =
                mapper.readTree(
                        getClass().getResourceAsStream("/invoice-template/default-layout.json"));
        var table =
                ((com.fasterxml.jackson.databind.node.ObjectNode) layout.path("body"))
                        .putArray("children")
                        .addObject()
                        .put("id", "testTable")
                        .put("type", "table")
                        .put("dataSource", dataset);
        table.putArray("columns")
                .addObject()
                .put("id", "code")
                .put("label", "Mã")
                .put("fieldKey", key)
                .put("widthPercent", 100);
        return layout;
    }

    @Test
    void readsNestedSelectorsInTheCorrectRow() throws Exception {
        var source = TestInvoiceFieldCatalog.snapshot(mapper);
        var fields = new ArrayList<>(source.fields());
        fields.add(
                new InvoiceCatalogSnapshot.Field(
                        1000L,
                        "items.NestedCode",
                        "Mã chi tiết",
                        "common",
                        "items",
                        true,
                        "XML",
                        "Detail/Code",
                        "STRING",
                        20,
                        "plainText",
                        "",
                        false,
                        true,
                        1000,
                        0L));
        var snapshot =
                new InvoiceCatalogSnapshot(
                        source.code(), source.groups(), source.datasets(), fields);
        InvoiceCatalogValidator.validate(snapshot);
        var result =
                new InvoiceViewModelMapper(new InvoiceFieldCatalog(snapshot))
                        .map(
                                "<HDon><DLHDon><NDHDon><DSHHDVu><HHDVu><Detail><Code>ONE</Code></Detail></HHDVu><HHDVu><Detail><Code>TWO</Code></Detail></HHDVu></DSHHDVu></NDHDon></DLHDon></HDon>",
                                Map.of(),
                                snapshot);
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) result.get("items");
        assertThat(rows.get(0).get("items.NestedCode")).isEqualTo("ONE");
        assertThat(rows.get(1).get("items.NestedCode")).isEqualTo("TWO");
        var layout =
                mapper.readTree(
                        getClass().getResourceAsStream("/invoice-template/default-layout.json"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) layout.path("body"))
                .putArray("children")
                .add(
                        mapper.readTree(
                                "{\"id\":\"table\",\"type\":\"table\",\"dataSource\":\"items\",\"columns\":[{\"id\":\"value\",\"label\":\"Mã\",\"fieldKey\":\"items.NestedCode\",\"widthPercent\":100}]}"));
        assertThat(
                        new JsonLayoutCompiler(new InvoiceFieldCatalog(snapshot))
                                .compile(layout, result))
                .contains("ONE", "TWO");
    }

    @Test
    void rejectsInvalidDatesWithAFieldValidationError() {
        var source = TestInvoiceFieldCatalog.snapshot(mapper);
        assertThatThrownBy(
                        () ->
                                new InvoiceViewModelMapper(new InvoiceFieldCatalog(source))
                                        .map(
                                                "<HDon><DLHDon><TTChung><NLap>not-a-date</NLap></TTChung></DLHDon></HDon>",
                                                Map.of(),
                                                source))
                .isInstanceOf(vn.hoadon.invoice.template.service.TemplateException.class)
                .hasMessageContaining("common.NLap");
    }

    @Test
    void rejectsDatasetCycles() {
        var source = TestInvoiceFieldCatalog.snapshot(mapper);
        var datasets = new ArrayList<>(source.datasets());
        int index =
                java.util.stream.IntStream.range(0, datasets.size())
                        .filter(i -> datasets.get(i).code().equals("items"))
                        .findFirst()
                        .orElseThrow();
        var item = datasets.get(index);
        datasets.set(
                index,
                new InvoiceCatalogSnapshot.Dataset(
                        item.id(),
                        item.code(),
                        item.label(),
                        "items.special",
                        "MANY",
                        "HHDVu",
                        true,
                        0L));
        assertThatThrownBy(
                        () ->
                                InvoiceCatalogValidator.validate(
                                        new InvoiceCatalogSnapshot(
                                                source.code(),
                                                source.groups(),
                                                datasets,
                                                source.fields())))
                .hasMessageContaining("vòng");
    }
}
