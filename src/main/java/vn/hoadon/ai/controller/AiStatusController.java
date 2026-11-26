package vn.hoadon.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.ai.config.AiProperties;
import vn.hoadon.ai.provider.AiProvider;
import vn.hoadon.controllers.base.BaseController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/ai")
public class AiStatusController extends BaseController {
    private final AiProperties properties;
    private final List<AiProvider> providers;

    public AiStatusController(AiProperties properties, List<AiProvider> providers) {
        this.properties = properties;
        this.providers = providers;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        if (currentUser() == null) {
            return ResponseEntity.status(401).build();
        }
        boolean configured = providers.stream()
                .filter(provider -> provider.name().equalsIgnoreCase(properties.getProvider()))
                .findFirst()
                .map(AiProvider::isConfigured)
                .orElse(false);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enabled", properties.isEnabled());
        response.put("provider", properties.getProvider());
        response.put("configured", configured);
        response.put("features", properties.featureStates());
        return ResponseEntity.ok(response);
    }
}
