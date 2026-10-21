package vn.hoadon.controllers.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.security.UserRoles;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/administrator/database")
public class DatabaseAdminController extends BaseController {

    private static final String SCHEMA = "dbo";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_DISPLAY_LENGTH = 5000;

    private static final Map<String, String> TABLE_LABELS = createTableLabels();

    private static final Set<String> READ_ONLY_TABLES = Set.of(
            "buy_invoice_histories",
            "history",
            "invoice_imports",
            "login_histories",
            "login_sessions",
            "mail_jobs",
            "notification_reads",
            "signature_authorities_tax",
            "signature_vats",
            "users"
    );

    private static final Set<String> HIDDEN_COLUMNS = Set.of(
            "password",
            "admin_password",
            "api_token",
            "remember_token",
            "forget_token",
            "password_hash",
            "bot_token",
            "mail_password"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/tables")
    public ResponseEntity<?> tables() {
        requireAdmin();

        List<Map<String, Object>> result = TABLE_LABELS.keySet().stream()
                .map(table -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", table);
                    item.put("label", TABLE_LABELS.get(table));
                    item.put("readOnly", READ_ONLY_TABLES.contains(table));
                    item.put("count", countRows(table));
                    return item;
                })
                .toList();

        return ResponseEntity.ok(Map.of("tables", result));
    }

    @GetMapping("/{table}/schema")
    public ResponseEntity<?> schema(@PathVariable String table) {
        requireAdmin();
        String tableName = validTable(table);
        return ResponseEntity.ok(schemaResponse(tableName));
    }

    @GetMapping("/{table}/rows")
    public ResponseEntity<?> rows(
            @PathVariable String table,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        requireAdmin();
        String tableName = validTable(table);
        List<ColumnInfo> columns = columns(tableName);
        if (columns.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bảng không có cột dữ liệu"));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String orderColumn = resolveOrderColumn(columns, sortBy);
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        List<ColumnInfo> searchable = columns.stream()
                .filter(column -> isSearchable(column.sqlType()))
                .toList();

        String where = "";
        List<Object> whereArgs = new ArrayList<>();
        if (keyword != null && !keyword.isBlank() && !searchable.isEmpty()) {
            String search = "%" + escapeLike(keyword.trim()) + "%";
            where = " WHERE " + searchable.stream()
                    .map(column -> "CAST(" + quote(column.name()) + " AS NVARCHAR(MAX)) LIKE ? ESCAPE '\\'")
                    .collect(Collectors.joining(" OR "));
            whereArgs.addAll(Collections.nCopies(searchable.size(), search));
        }

        String qualifiedTable = qualified(tableName);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT_BIG(*) FROM " + qualifiedTable + where,
                whereArgs.toArray(),
                Long.class);

        String selectColumns = columns.stream()
                .filter(column -> !HIDDEN_COLUMNS.contains(column.name().toLowerCase(Locale.ROOT)))
                .map(column -> quote(column.name()))
                .collect(Collectors.joining(", "));

        String sql = "SELECT " + selectColumns + " FROM " + qualifiedTable + where
                + " ORDER BY " + quote(orderColumn) + " " + direction
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> args = new ArrayList<>(whereArgs);
        args.add(safePage * safeSize);
        args.add(safeSize);

        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, args.toArray()).stream()
                .map(this::displayRow)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        response.put("page", safePage);
        response.put("size", safeSize);
        response.put("total", total == null ? 0 : total);
        response.put("table", tableName);
        response.put("readOnly", READ_ONLY_TABLES.contains(tableName));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{table}/rows/{id}")
    public ResponseEntity<?> row(@PathVariable String table, @PathVariable String id) {
        requireAdmin();
        String tableName = validTable(table);
        PrimaryKey primaryKey = primaryKey(tableName);
        Object key = parseValue(id, primaryKey.column().sqlType());
        String sql = "SELECT " + visibleColumnList(columns(tableName)) + " FROM " + qualified(tableName)
                + " WHERE " + quote(primaryKey.column().name()) + " = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, key);
        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(displayRow(rows.get(0)));
    }

    @PostMapping("/{table}/rows")
    public ResponseEntity<?> insert(@PathVariable String table, @RequestBody Map<String, Object> body) {
        requireAdmin();
        String tableName = validTable(table);
        requireWritable(tableName);
        List<ColumnInfo> columns = writableColumns(tableName, false);
        Map<String, Object> values = normalizeValues(columns, body);
        addAuditDefaults(columns, values, false);
        if (values.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chưa có dữ liệu để thêm"));
        }

        String names = values.keySet().stream().map(this::quote).collect(Collectors.joining(", "));
        String placeholders = String.join(", ", Collections.nCopies(values.size(), "?"));
        String sql = "INSERT INTO " + qualified(tableName) + " (" + names + ") VALUES (" + placeholders + ")";
        jdbcTemplate.update(sql, values.values().toArray());
        return ResponseEntity.ok(Map.of("message", "Đã thêm dữ liệu"));
    }

    @PutMapping("/{table}/rows/{id}")
    public ResponseEntity<?> update(
            @PathVariable String table,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        requireAdmin();
        String tableName = validTable(table);
        requireWritable(tableName);
        PrimaryKey primaryKey = primaryKey(tableName);
        Object key = parseValue(id, primaryKey.column().sqlType());
        List<ColumnInfo> columns = writableColumns(tableName, true);
        Map<String, Object> values = normalizeValues(columns, body);
        addAuditDefaults(columns, values, true);
        values.remove(primaryKey.column().name());
        if (values.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chưa có dữ liệu để cập nhật"));
        }

        String assignments = values.keySet().stream()
                .map(name -> quote(name) + " = ?")
                .collect(Collectors.joining(", "));
        List<Object> args = new ArrayList<>(values.values());
        args.add(key);
        String sql = "UPDATE " + qualified(tableName) + " SET " + assignments
                + " WHERE " + quote(primaryKey.column().name()) + " = ?";
        int affected = jdbcTemplate.update(sql, args.toArray());
        if (affected == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật dữ liệu"));
    }

    @DeleteMapping("/{table}/rows/{id}")
    public ResponseEntity<?> delete(@PathVariable String table, @PathVariable String id) {
        requireAdmin();
        String tableName = validTable(table);
        requireWritable(tableName);
        PrimaryKey primaryKey = primaryKey(tableName);
        Object key = parseValue(id, primaryKey.column().sqlType());
        int affected = jdbcTemplate.update(
                "DELETE FROM " + qualified(tableName) + " WHERE " + quote(primaryKey.column().name()) + " = ?",
                key);
        if (affected == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Đã xóa dữ liệu"));
    }

    private void requireAdmin() {
        if (currentUser() == null || !UserRoles.canAccessAdminArea(currentUser().getRole())) {
            throw new AccessDeniedException("Chỉ quản trị viên hệ thống mới được quản lý dữ liệu database");
        }
    }

    private void requireWritable(String table) {
        if (READ_ONLY_TABLES.contains(table)) {
            throw new AccessDeniedException("Bảng này chỉ cho phép xem dữ liệu");
        }
    }

    private String validTable(String table) {
        String tableName = table == null ? "" : table.trim().toLowerCase(Locale.ROOT);
        if (!TABLE_LABELS.containsKey(tableName)) {
            throw new IllegalArgumentException("Bảng không được phép truy cập");
        }
        return tableName;
    }

    private Map<String, Object> schemaResponse(String table) {
        List<ColumnInfo> columns = columns(table);
        List<Map<String, Object>> result = columns.stream().map(column -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", column.name());
            item.put("sqlType", column.sqlType());
            item.put("type", column.type());
            item.put("nullable", column.nullable());
            item.put("identity", column.identity());
            item.put("primaryKey", column.primaryKey());
            item.put("writable", !column.identity()
                    && !HIDDEN_COLUMNS.contains(column.name().toLowerCase(Locale.ROOT))
                    && !READ_ONLY_TABLES.contains(table));
            return item;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", table);
        response.put("label", TABLE_LABELS.get(table));
        response.put("readOnly", READ_ONLY_TABLES.contains(table));
        response.put("columns", result);
        return response;
    }

    private List<ColumnInfo> columns(String table) {
        return jdbcTemplate.execute((Connection connection) -> {
            DatabaseMetaData metadata = connection.getMetaData();
            Set<String> primaryKeys = new HashSet<>();
            try (ResultSet rs = metadata.getPrimaryKeys(connection.getCatalog(), SCHEMA, table)) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }

            List<ColumnInfo> result = new ArrayList<>();
            try (ResultSet rs = metadata.getColumns(connection.getCatalog(), SCHEMA, table, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    String sqlType = rs.getString("TYPE_NAME");
                    String normalizedType = sqlType == null ? "" : sqlType.toLowerCase(Locale.ROOT);
                    boolean identity = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                    result.add(new ColumnInfo(
                            name,
                            normalizedType,
                            displayType(normalizedType),
                            "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")),
                            identity,
                            primaryKeys.contains(name.toLowerCase(Locale.ROOT)),
                            rs.getInt("ORDINAL_POSITION")));
                }
            }
            result.sort(Comparator.comparingInt(ColumnInfo::ordinal));
            return result;
        });
    }

    private PrimaryKey primaryKey(String table) {
        return columns(table).stream()
                .filter(ColumnInfo::primaryKey)
                .findFirst()
                .map(column -> new PrimaryKey(column))
                .orElseThrow(() -> new IllegalArgumentException("Bảng không có khóa chính"));
    }

    private long countRows(String table) {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM " + qualified(table), Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException ignored) {
            return 0;
        }
    }

    private List<ColumnInfo> writableColumns(String table, boolean includePrimaryKey) {
        return columns(table).stream()
                .filter(column -> !column.identity())
                .filter(column -> includePrimaryKey || !column.primaryKey())
                .filter(column -> !HIDDEN_COLUMNS.contains(column.name().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private Map<String, Object> normalizeValues(List<ColumnInfo> columns, Map<String, Object> body) {
        Map<String, ColumnInfo> byName = columns.stream()
                .collect(Collectors.toMap(ColumnInfo::name, column -> column, (a, b) -> a, LinkedHashMap::new));
        Map<String, Object> values = new LinkedHashMap<>();
        if (body == null) return values;

        body.forEach((rawName, rawValue) -> {
            ColumnInfo column = byName.get(rawName);
            if (column == null) return;
            values.put(column.name(), convertValue(rawValue, column.sqlType()));
        });
        return values;
    }

    private void addAuditDefaults(List<ColumnInfo> columns, Map<String, Object> values, boolean update) {
        Set<String> names = columns.stream().map(ColumnInfo::name).collect(Collectors.toSet());
        if (names.contains("created_at") && !update && !values.containsKey("created_at")) {
            values.put("created_at", Timestamp.valueOf(LocalDateTime.now()));
        }
        if (names.contains("updated_at") && !values.containsKey("updated_at")) {
            values.put("updated_at", Timestamp.valueOf(LocalDateTime.now()));
        }
    }

    private Object convertValue(Object value, String sqlType) {
        if (value == null) return null;
        if (value instanceof Map<?, ?> || value instanceof Collection<?>) {
            try {
                value = objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Dữ liệu JSON không hợp lệ");
            }
        }
        String text = String.valueOf(value);
        if (text.isBlank()) return null;

        try {
            return switch (sqlType.toLowerCase(Locale.ROOT)) {
                case "bit" -> value instanceof Boolean ? value : ("1".equals(text) || "true".equalsIgnoreCase(text));
                case "tinyint" -> Byte.valueOf(text);
                case "smallint" -> Short.valueOf(text);
                case "int" -> Integer.valueOf(text);
                case "bigint" -> Long.valueOf(text);
                case "decimal", "numeric", "money", "smallmoney" -> new BigDecimal(text);
                case "float", "real" -> Double.valueOf(text);
                case "date" -> Date.valueOf(LocalDate.parse(text.substring(0, 10)));
                case "time" -> java.sql.Time.valueOf(LocalTime.parse(text.length() >= 8 ? text.substring(0, 8) : text));
                case "datetime", "datetime2", "smalldatetime", "datetimeoffset" -> Timestamp.valueOf(normalizeDateTime(text));
                default -> text;
            };
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Giá trị không hợp lệ cho kiểu " + sqlType + ": " + text);
        }
    }

    private Object parseValue(String value, String sqlType) {
        return convertValue(value, sqlType);
    }

    private String normalizeDateTime(String value) {
        String text = value.trim().replace('T', ' ');
        if (text.length() == 10) return text + " 00:00:00";
        if (text.length() == 16) return text + ":00";
        return text;
    }

    private Map<String, Object> displayRow(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            if (HIDDEN_COLUMNS.contains(key.toLowerCase(Locale.ROOT))) return;
            if (value instanceof String text && text.length() > MAX_DISPLAY_LENGTH) {
                result.put(key, text.substring(0, MAX_DISPLAY_LENGTH) + "…");
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    private String visibleColumnList(List<ColumnInfo> columns) {
        return columns.stream()
                .filter(column -> !HIDDEN_COLUMNS.contains(column.name().toLowerCase(Locale.ROOT)))
                .map(column -> quote(column.name()))
                .collect(Collectors.joining(", "));
    }

    private String resolveOrderColumn(List<ColumnInfo> columns, String requested) {
        if (requested != null) {
            Optional<ColumnInfo> found = columns.stream()
                    .filter(column -> column.name().equals(requested))
                    .findFirst();
            if (found.isPresent()) return found.get().name();
        }
        return columns.stream().filter(ColumnInfo::primaryKey).map(ColumnInfo::name)
                .findFirst().orElse(columns.get(0).name());
    }

    private boolean isSearchable(String type) {
        return Set.of("varchar", "nvarchar", "char", "nchar", "text", "ntext", "uniqueidentifier").contains(type);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String qualified(String table) {
        return quote(SCHEMA) + "." + quote(table);
    }

    private String quote(String identifier) {
        return "[" + identifier.replace("]", "]]" ) + "]";
    }

    private String displayType(String sqlType) {
        return switch (sqlType) {
            case "nvarchar", "varchar", "nchar", "char", "text", "ntext" -> "text";
            case "bit" -> "boolean";
            case "tinyint", "smallint", "int", "bigint" -> "number";
            case "decimal", "numeric", "money", "smallmoney", "float", "real" -> "decimal";
            case "date" -> "date";
            case "datetime", "datetime2", "smalldatetime", "datetimeoffset" -> "datetime";
            default -> "text";
        };
    }

    private static Map<String, String> createTableLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("banks", "Ngân hàng");
        labels.put("tax_authorities", "Cơ quan thuế");
        labels.put("provinces", "Tỉnh thành");
        labels.put("companies", "Công ty");
        labels.put("users", "Tài khoản");
        labels.put("company_allowed_ips", "IP được phép");
        labels.put("company_banks", "Tài khoản ngân hàng");
        labels.put("legal_representatives", "Người đại diện pháp luật");
        labels.put("company_registration_requests", "Yêu cầu đăng ký công ty");
        labels.put("customers", "Khách hàng");
        labels.put("products", "Sản phẩm");
        labels.put("form_invoices", "Mẫu hóa đơn");
        labels.put("invoice_numbers", "Số hóa đơn");
        labels.put("invoices", "Hóa đơn");
        labels.put("hash_invoices", "Mã băm hóa đơn");
        labels.put("error_messages", "Thông báo sai sót");
        labels.put("error_message_invoices", "Hóa đơn trong thông báo sai sót");
        labels.put("invoice_imports", "Lần import hóa đơn");
        labels.put("register_invoices", "Tờ khai hóa đơn");
        labels.put("signature_vats", "Chữ ký VAT");
        labels.put("signature_authorities_tax", "Chữ ký cơ quan thuế");
        labels.put("invoice_packages", "Gói hóa đơn");
        labels.put("invoice_package_purchases", "Lịch sử mua gói");
        labels.put("buy_invoices", "Hạn mức hóa đơn");
        labels.put("buy_invoice_histories", "Lịch sử hạn mức");
        labels.put("vat_rates", "Thuế suất");
        labels.put("permission_categories", "Nhóm quyền");
        labels.put("permissions", "Quyền");
        labels.put("user_permissions", "Quyền tài khoản");
        labels.put("mail_servers", "Máy chủ mail");
        labels.put("mail_templates", "Mẫu email");
        labels.put("mail_jobs", "Hàng đợi email");
        labels.put("history", "Lịch sử hệ thống");
        labels.put("notification_reads", "Trạng thái thông báo");
        labels.put("login_histories", "Lịch sử đăng nhập");
        labels.put("login_sessions", "Phiên đăng nhập");
        labels.put("telegram_configs", "Cấu hình Telegram");
        labels.put("daily_invoice_report_configs", "Cấu hình báo cáo ngày");
        return Collections.unmodifiableMap(labels);
    }

    private record ColumnInfo(
            String name,
            String sqlType,
            String type,
            boolean nullable,
            boolean identity,
            boolean primaryKey,
            int ordinal) {
    }

    private record PrimaryKey(ColumnInfo column) {
    }
}
