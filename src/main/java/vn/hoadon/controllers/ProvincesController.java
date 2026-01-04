package vn.hoadon.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.entity.ProvinceEntity;
import vn.hoadon.services.ProvinceService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/provinces")
public class ProvincesController {

    private final ProvinceService provinceService;

    public ProvincesController(ProvinceService provinceService) {
        this.provinceService = provinceService;
    }

    @GetMapping
    public List<ProvinceDTO> getActive() {
        List<ProvinceEntity> provinces = provinceService.getActiveProvinces();
        return provinces.stream()
                .map(p -> new ProvinceDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }

    public static class ProvinceDTO {
        private Integer id;
        private String name;
        public ProvinceDTO(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
        public Integer getId() { return id; }
        public String getName() { return name; }
    }
}