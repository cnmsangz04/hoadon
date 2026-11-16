package vn.hoadon.invoice.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.playwright.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.compiler.JsonLayoutCompiler;

import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "invoice.designer.browser-tests", matches = "true")
class InvoiceDesignerBrowserTest {
    private static HttpServer server;
    private static Playwright playwright;
    private static Browser browser;
    private final ObjectMapper mapper = new ObjectMapper();
    private BrowserContext context;
    private Page page;
    private ObjectNode draft;
    private int revision;
    private String defaultSource;
    private InvoiceFieldCatalog catalog;
    private JsonLayoutCompiler compiler;

    @BeforeAll
    static void start() throws Exception {
        Path root = Path.of(System.getProperty("invoice.designer.bundle", "target/invoice-designer-test")).toAbsolutePath();
        org.assertj.core.api.Assertions.assertThat(root.resolve("index.html")).exists();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            Path file = root.resolve(exchange.getRequestURI().getPath().substring(1)).normalize();
            if (Files.isDirectory(file)) file = file.resolve("index.html");
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                String type = file.toString().endsWith(".js") ? "application/javascript" : file.toString().endsWith(".css") ? "text/css" : "text/html";
                exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
                byte[] bytes = Files.readAllBytes(file);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @BeforeEach
    void open() throws Exception {
        catalog = TestInvoiceFieldCatalog.create(mapper);
        compiler = new JsonLayoutCompiler(catalog);
        try (var stream = getClass().getResourceAsStream("/invoice-template/default-layout.json")) {
            defaultSource = mapper.writeValueAsString(mapper.readTree(stream));
        }
        draft = mapper.createObjectNode().put("id", 1).put("sourceContent", defaultSource).put("fieldCatalogVersion", catalog.version()).put("lockVersion", 0);
        revision = 0;
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1600, 1400));
        context.route("**/v1/**", route -> {
            try { respond(route); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        page = context.newPage();
        page.navigate("http://127.0.0.1:" + server.getAddress().getPort());
        assertThat(frame().locator("[data-block-id=block7]")).containsText("CÔNG TY MINH HỌA");
        page.waitForFunction("() => window.invoiceDesigner.$children.some(c => c.$options.name === 'InvoiceLayoutCanvas' && c.observer && !c.loading)");
    }

    private FrameLocator frame() { return page.frameLocator("iframe[title='Trang thiết kế mẫu hóa đơn']"); }

    private void respond(Route route) throws Exception {
        String path = java.net.URI.create(route.request().url()).getPath();
        Object data;
        if (path.endsWith("/catalog")) data = Map.of("version", catalog.version(), "fields", catalog.latest().fields(), "datasets", catalog.latest().datasets(), "groups", catalog.latest().groups());
        else if (path.endsWith("/designer-fonts")) data = Map.of("css", "");
        else if (path.endsWith("/designer-preview")) {
            var input = mapper.readTree(route.request().postData());
            var xml = new InvoiceFixtureFactory(mapper).sample(1, input.path("rows").asInt());
            data = Map.of("html", compiler.compile(input.path("layout"), new InvoiceViewModelMapper(catalog).map(xml, Map.of("sampleMarker", "SAMPLE"))));
        } else if (path.endsWith("/draft") && route.request().method().equals("PUT")) {
            var input = mapper.readTree(route.request().postData());
            draft.put("sourceContent", mapper.writeValueAsString(input.path("layout"))).put("lockVersion", ++revision);
            data = draft;
        } else if (path.endsWith("/invoice-templates")) data = Map.of("content", List.of(Map.of("id", 1, "name", "Mẫu kiểm thử")), "totalElements", 1);
        else if (path.endsWith("/default-layout")) data = mapper.readTree(defaultSource);
        else data = Map.of("template", Map.of("id", 1, "name", "Mẫu kiểm thử", "category", 1), "draft", draft, "versions", List.of());
        route.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody(mapper.writeValueAsString(data)));
    }

    @Test
    void hiddenFieldsStayAvailableOnlyForExistingColumns() {
        var result = page.evaluate("""
                () => {
                  const designer = window.invoiceDesigner;
                  designer.fields = [
                    {key: 'items.hidden', label: 'Đã ẩn', dataset: 'items', rowScope: 'items', active: false},
                    {key: 'items.detail.Code', label: 'Chi tiết', dataset: 'items.detail', rowScope: 'items', active: true}
                  ];
                  return {
                    fresh: designer.fieldsFor('items').map(field => field.value),
                    existing: designer.fieldsFor('items', 'items.hidden').map(field => field.value)
                  };
                }
                """);
        @SuppressWarnings("unchecked") var values = (Map<String, Object>) result;
        assertThat(values.get("fresh")).isEqualTo(List.of("items.detail.Code"));
        assertThat(values.get("existing")).isEqualTo(List.of("items.hidden", "items.detail.Code"));
    }

    @Test
    void selectsFormatsAndUpdatesTheDocumentWithoutSaving() {
        clickBlock("block6", false);
        page.getByTitle("In đậm", new Page.GetByTitleOptions().setExact(true)).click();
        assertThat(frame().locator("[data-block-id=block6]")).hasCSS("font-weight", "700");
        assertThat(page.locator(".document-header .badge")).hasText("Chưa lưu");
        assertThat(revision).isZero();
        page.getByTitle("Hoàn tác (Ctrl+Z)").click();
        assertThat(frame().locator("[data-block-id=block6]")).hasCSS("font-weight", "400");
        page.getByTitle("Làm lại (Ctrl+Y)").click();
        assertThat(frame().locator("[data-block-id=block6]")).hasCSS("font-weight", "700");
    }

    @Test
    void editsTextOnThePageAndPersistsItAsPlainText() throws Exception {
        var text = frame().locator("[data-block-id=block25]");
        clickBlock("block25", true);
        text.fill("NGƯỜI MUA <kiểm tra>");
        clickBlock("block7", false);
        assertThat(frame().locator("[data-block-id=block25]")).hasText("NGƯỜI MUA <kiểm tra>");
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Lưu nháp").setExact(true)).click();
        assertThat(page.locator(".document-header .badge")).hasText("Đã lưu");
        assertThat(draft.path("sourceContent").asText()).contains("NGƯỜI MUA <kiểm tra>");
        assertThat(frame().locator("[data-block-id=block25] kiểm")).hasCount(0);
    }

    @Test
    void pointerDragReordersBlocksOnThePage() {
        drag("block6", "block1", 0.5, 0.05);
        page.waitForFunction("() => { const blocks = window.invoiceDesigner.layout.body.children; return blocks.findIndex(b => b.id === 'block6') + 1 === blocks.findIndex(b => b.id === 'block1'); }",
                null, new Page.WaitForFunctionOptions().setTimeout(5000));
        assertThat(frame().locator("[data-block-id=block6]")).hasCount(1);
    }

    @Test
    void dragsBlocksBetweenGroupsAndCanUndoTheMove() {
        drag("block6", "block27", 0.5, 0.5);
        page.waitForFunction("() => window.invoiceDesigner.layout.body.children.find(b => b.id === 'block27').children.some(b => b.id === 'block6')");
        assertThat(frame().locator("[data-block-id=block27] [data-block-id=block6]")).hasCount(1);
        page.getByTitle("Hoàn tác (Ctrl+Z)").click();
        assertThat(frame().locator("[data-block-id=block27] [data-block-id=block6]")).hasCount(0);
        assertThat(frame().locator("[data-block-id=block6]")).hasCount(1);
    }

    private void clickBlock(String id, boolean twice) {
        var box = frame().locator("[data-block-id=" + id + "]").boundingBox();
        if (twice) page.mouse().dblclick(box.x + box.width / 2, box.y + box.height / 2);
        else page.mouse().click(box.x + box.width / 2, box.y + box.height / 2);
    }

    private void drag(String sourceId, String targetId, double x, double y) {
        var source = frame().locator("[data-block-id=" + sourceId + "]").boundingBox();
        var target = frame().locator("[data-block-id=" + targetId + "]").boundingBox();
        page.mouse().move(source.x + source.width / 2, source.y + source.height / 2);
        page.mouse().down();
        page.mouse().move(target.x + target.width * x, target.y + target.height * y, new Mouse.MoveOptions().setSteps(12));
        page.mouse().up();
    }

    @Test
    void droppingAtTheSideCreatesAColumnGroup() {
        drag("block6", "block1", 0.03, 0.5);
        page.waitForFunction("() => window.invoiceDesigner.layout.body.children.some(b => b.type === 'columns' && b.children[0]?.id === 'block6' && b.children[1]?.id === 'block1')");
        assertThat(frame().locator("[data-block-id=block1]")).hasCount(1);
    }

    @Test
    void insertsFromPaletteAndDuplicatesAndDeletesTheSelection() {
        page.locator(".block-palette button").filter(new Locator.FilterOptions().setHasText("Văn bản")).click();
        assertThat(frame().locator("[data-editor-selected]")).hasText("Nội dung");
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Nhân đôi").setExact(true)).click();
        assertThat(frame().locator(".text").filter(new Locator.FilterOptions().setHasText("Nội dung"))).hasCount(2);
        page.locator(".format-toolbar button").filter(new Locator.FilterOptions().setHasText("Xóa")).click();
        assertThat(frame().locator(".text").filter(new Locator.FilterOptions().setHasText("Nội dung"))).hasCount(1);
    }

    @Test
    void changesPaperOrientationAndZoomWithoutChangingTheLayoutScale() {
        page.getByRole(com.microsoft.playwright.options.AriaRole.TAB, new Page.GetByRoleOptions().setName("Bố cục trang")).click();
        page.locator(".page-toolbar select").nth(1).selectOption("landscape");
        assertThat(frame().locator("body")).hasCSS("width", "1122.52px");
        page.locator("#invoice-designer-zoom").focus();
        page.locator("#invoice-designer-zoom").press("Home");
        for (int i = 0; i < 4; i++) page.locator("#invoice-designer-zoom").press("ArrowRight");
        assertThat(page.locator(".canvas-sheet")).hasCSS("transform", "matrix(0.6, 0, 0, 0.6, 0, 0)");
        assertThat(frame().locator("body")).hasCSS("width", "1122.52px");
    }

    @AfterEach
    void closeContext() {
        if (page != null) page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(System.getProperty("invoice.designer.bundle", "target/invoice-designer-test"), "designer.png")).setFullPage(true));
        if (context != null) context.close();
    }

    @AfterAll
    static void close() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        if (server != null) server.stop(0);
    }
}
