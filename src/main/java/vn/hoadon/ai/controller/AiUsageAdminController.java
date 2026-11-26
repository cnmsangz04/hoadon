package vn.hoadon.ai.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.ai.persistence.AiUsageLogEntity;
import vn.hoadon.ai.service.AiUsageQueryService;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.security.UserRoles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/administrator/ai/usage")
public class AiUsageAdminController extends BaseController {
    private static final int MAX_PAGE_SIZE = 100;

    private final AiUsageQueryService queryService;

    public AiUsageAdminController(AiUsageQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin();
        DateRange range = range(dateFrom, dateTo);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Page<AiUsageLogEntity> result = queryService.find(companyId, range.from(), range.to(),
                PageRequest.of(safePage - 1, safeSize));

        List<Map<String, Object>> items = result.getContent().stream().map(this::toItem).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("current_page", safePage);
        response.put("per_page", safeSize);
        response.put("total", result.getTotalElements());
        response.put("last_page", Math.max(1, result.getTotalPages()));
        response.put("summary", queryService.summarize(companyId, range.from(), range.to()));
        return ResponseEntity.ok(response);
    }

    private DateRange range(LocalDate dateFrom, LocalDate dateTo) {
        LocalDate today = LocalDate.now();
        LocalDate from = dateFrom == null ? today.minusDays(29) : dateFrom;
        LocalDate through = dateTo == null ? today : dateTo;
        if (through.isBefore(from)) {
            throw new IllegalArgumentException("Khoảng ngày thống kê AI không hợp lệ");
        }
        return new DateRange(from.atStartOfDay(), through.plusDays(1).atStartOfDay());
    }

    private Map<String, Object> toItem(AiUsageLogEntity entity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("requestId", entity.getRequestId());
        item.put("companyId", entity.getCompanyId());
        item.put("userId", entity.getUserId());
        item.put("feature", entity.getFeature());
        item.put("provider", entity.getProvider());
        item.put("model", entity.getModel());
        item.put("status", entity.getStatus());
        item.put("errorCode", entity.getErrorCode());
        item.put("inputCharacters", entity.getInputCharacters());
        item.put("inputTokens", entity.getInputTokens());
        item.put("outputTokens", entity.getOutputTokens());
        item.put("totalTokens", entity.getTotalTokens());
        item.put("durationMs", entity.getDurationMs());
        item.put("attemptCount", entity.getAttemptCount());
        item.put("createdAt", entity.getCreatedAt());
        return item;
    }

    private void requireAdmin() {
        if (currentUser() == null || !UserRoles.canAccessAdminArea(currentUser().getRole())) {
            throw new AccessDeniedException("Chỉ quản trị viên hệ thống mới được xem thống kê AI");
        }
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {
    }
}
