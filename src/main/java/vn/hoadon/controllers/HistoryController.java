package vn.hoadon.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.history.HistoryDto;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.HistoryService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/history")
public class HistoryController extends BaseController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
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
        // Pull recent by company, then filter by visibility flags per requirements and cap the limit
        List<HistoryDto> rows = historyService.listRecentByCompany(user.getCompanyId(), Math.max(1, limit));
        return rows.stream()
                .filter(h -> (h.getShowNotify() != null ? h.getShowNotify() : 0) == showNotify)
                .filter(h -> (h.getStatus() != null ? h.getStatus() : 0) == status)
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }
}
