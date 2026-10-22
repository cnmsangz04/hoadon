package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.security.UserRoles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/administrator/logs")
public class LogAdminController extends BaseController {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_READ_LINES = 20000;
    private static final int MAX_ENTRY_LENGTH = 20000;
    private static final Pattern LOG_ENTRY = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T ][^ ]+)\\s+(TRACE|DEBUG|INFO|WARN|ERROR)\\s+\\d+\\s+---\\s+"
                    + "(?:\\[[^]]*\\]\\s+)?\\[([^]]*)\\]\\s+(.+?)\\s+:\\s?(.*)$");

    @Value("${logging.file.name:logs/hoadon.log}")
    private String configuredLogFile;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin();

        Path path = resolveLogPath();
        List<LogEntry> entries = readEntries(path);
        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String levelFilter = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        LocalDate filterFrom = parseDate(dateFrom);
        LocalDate filterTo = parseDate(dateTo);

        List<LogEntry> filtered = entries.stream()
                .filter(entry -> levelFilter.isBlank() || "ALL".equals(levelFilter)
                        || levelFilter.equals(entry.level()))
                .filter(entry -> filterFrom == null || (entry.date() != null && !entry.date().isBefore(filterFrom)))
                .filter(entry -> filterTo == null || (entry.date() != null && !entry.date().isAfter(filterTo)))
                .filter(entry -> keyword.isBlank() || entry.raw().toLowerCase(Locale.ROOT).contains(keyword))
                .toList();

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int total = filtered.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<Map<String, Object>> items = filtered.subList(from, to).stream()
                .map(LogEntry::toMap)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("current_page", safePage);
        response.put("per_page", safeSize);
        response.put("total", total);
        response.put("last_page", Math.max(1, (int) Math.ceil(total / (double) safeSize)));
        response.put("fileName", path.getFileName().toString());
        response.put("exists", Files.exists(path));
        response.put("lastModified", lastModified(path));
        return ResponseEntity.ok(response);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<LogEntry> readEntries(Path path) {
        if (!Files.isRegularFile(path)) return List.of();
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int start = Math.max(0, lines.size() - MAX_READ_LINES);
            List<LogEntry> entries = new ArrayList<>();
            LogEntry current = null;
            for (String line : lines.subList(start, lines.size())) {
                Matcher matcher = LOG_ENTRY.matcher(line);
                if (matcher.matches()) {
                    current = new LogEntry(matcher.group(1), matcher.group(2), matcher.group(3),
                            matcher.group(4), line);
                    entries.add(current);
                } else if (current != null) {
                    current.append(line);
                } else if (!line.isBlank()) {
                    entries.add(new LogEntry("", "", "", line, line));
                }
            }
            Collections.reverse(entries);
            return entries;
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private Path resolveLogPath() {
        Path configured = Paths.get(configuredLogFile == null || configuredLogFile.isBlank()
                ? "logs/hoadon.log" : configuredLogFile);
        return configured.isAbsolute()
                ? configured.normalize()
                : Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
    }

    private String lastModified(Path path) {
        try {
            return Files.exists(path) ? Files.getLastModifiedTime(path).toInstant().toString() : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private void requireAdmin() {
        if (currentUser() == null || !UserRoles.canAccessAdminArea(currentUser().getRole())) {
            throw new AccessDeniedException("Chỉ quản trị viên hệ thống mới được xem log");
        }
    }

    private static final class LogEntry {
        private final String timestamp;
        private final String level;
        private final String thread;
        private final String logger;
        private final StringBuilder raw;
        private String message;

        private LogEntry(String timestamp, String level, String thread, String logger, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.thread = thread;
            this.logger = logger;
            this.message = message;
            this.raw = new StringBuilder(message);
        }

        private void append(String line) {
            int rawRemaining = MAX_ENTRY_LENGTH - raw.length();
            if (rawRemaining > 1) {
                raw.append('\n');
                int contentLength = Math.min(line.length(), MAX_ENTRY_LENGTH - raw.length());
                if (contentLength > 0) raw.append(line, 0, contentLength);
            }
            int messageRemaining = MAX_ENTRY_LENGTH - message.length();
            if (messageRemaining > 1) {
                int contentLength = Math.min(line.length(), messageRemaining - 1);
                message = message + "\n" + line.substring(0, contentLength);
            }
        }

        private String raw() {
            return raw.toString();
        }

        private String level() {
            return level;
        }

        private LocalDate date() {
            if (timestamp == null || timestamp.length() < 10) return null;
            try {
                return LocalDate.parse(timestamp.substring(0, 10));
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        private Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", timestamp);
            row.put("level", level);
            row.put("thread", thread);
            row.put("logger", logger);
            row.put("message", message);
            row.put("raw", raw());
            return row;
        }
    }
}
