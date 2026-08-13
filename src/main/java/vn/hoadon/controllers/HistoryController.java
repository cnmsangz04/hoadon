package vn.hoadon.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    // GET /v1/history/notifications?page=1&size=10&show_notify=1&status=1
    @GetMapping("/notifications")
    public Map<String, Object> notifications(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "show_notify", required = false, defaultValue = "1") int showNotify,
            @RequestParam(name = "status", required = false, defaultValue = "1") int status
    ) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return Map.of(
                    "items", List.of(),
                    "current_page", 1,
                    "per_page", 10,
                    "total", 0L,
                    "last_page", 0,
                    "has_more", false,
                    "unread_count", 0L
            );
        }
        int currentPage = Math.max(1, page != null ? page : 1);
        int pageSize = Math.max(1, Math.min(100, size != null ? size : (limit != null ? limit : 10)));
        Page<HistoryDto> result = historyService.pageNotificationsByCompany(
                user.getCompanyId(),
                showNotify,
                status,
                PageRequest.of(currentPage - 1, pageSize)
        );
        List<HistoryDto> items = result.getContent();

        Set<Long> ids = items.stream()
                .map(HistoryDto::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> readIds = ids.isEmpty()
                ? Set.of()
                : notificationReadRepository.findByUserIdAndHistoryIdIn(user.getId(), ids)
                        .stream()
                        .map(NotificationReadEntity::getHistoryId)
                        .collect(Collectors.toSet());
        items.forEach(h -> h.setRead(h.getId() != null && readIds.contains(h.getId())));
        long unreadCount = historyService.countUnreadNotifications(user.getCompanyId(), user.getId(), showNotify, status);

        return Map.of(
                "items", items,
                "current_page", result.getNumber() + 1,
                "per_page", result.getSize(),
                "total", result.getTotalElements(),
                "last_page", result.getTotalPages(),
                "has_more", result.hasNext(),
                "unread_count", unreadCount
        );
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
