package vn.hoadon.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.history.HistoryDto;
import vn.hoadon.entity.NotificationReadEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.NotificationReadRepository;
import vn.hoadon.services.HistoryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/history")
public class HistoryController extends BaseController {

    private final HistoryService historyService;
    private final NotificationReadRepository notificationReadRepository;

    public HistoryController(HistoryService historyService, NotificationReadRepository notificationReadRepository) {
        this.historyService = historyService;
        this.notificationReadRepository = notificationReadRepository;
    }

    // GET /v1/history/notifications?limit=10&show_notify=1&status=1
    @GetMapping("/notifications")
    public List<HistoryDto> notifications(
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @RequestParam(name = "show_notify", required = false, defaultValue = "1") int showNotify,
            @RequestParam(name = "status", required = false, defaultValue = "1") int status
    ) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return List.of();
        }
        // Lấy dữ liệu gần đây theo công ty, rồi lọc theo cờ hiển thị và giới hạn số lượng
        List<HistoryDto> rows = historyService.listRecentByCompany(user.getCompanyId(), Math.max(1, limit));
        List<HistoryDto> filtered = rows.stream()
                .filter(h -> (h.getShowNotify() != null ? h.getShowNotify() : 0) == showNotify)
                .filter(h -> (h.getStatus() != null ? h.getStatus() : 0) == status)
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());

        Set<Long> ids = filtered.stream()
                .map(HistoryDto::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> readIds = ids.isEmpty()
                ? Set.of()
                : notificationReadRepository.findByUserIdAndHistoryIdIn(user.getId(), ids)
                        .stream()
                        .map(NotificationReadEntity::getHistoryId)
                        .collect(Collectors.toSet());
        filtered.forEach(h -> h.setRead(h.getId() != null && readIds.contains(h.getId())));
        return filtered;
    }

    @PostMapping("/notifications/read")
    public ResponseEntity<?> markNotificationsRead(@RequestBody(required = false) Map<String, Object> body) {
        UserEntity user = currentUser();
        if (user == null || user.getId() == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Không xác định được người dùng"));
        }

        List<Long> ids = extractIds(body);
        for (Long id : ids) {
            if (id == null) continue;
            notificationReadRepository.findByUserIdAndHistoryId(user.getId(), id).orElseGet(() -> {
                NotificationReadEntity read = new NotificationReadEntity();
                read.setUserId(user.getId());
                read.setCompanyId(user.getCompanyId());
                read.setHistoryId(id);
                read.setReadAt(LocalDateTime.now());
                return notificationReadRepository.save(read);
            });
        }
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu thông báo đã đọc", "count", ids.size()));
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractIds(Map<String, Object> body) {
        if (body == null) return List.of();
        Object raw = body.get("ids");
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream()
                .map(value -> {
                    if (value instanceof Number n) return n.longValue();
                    try { return Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return null; }
                })
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
    }
}
