package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.hoadon.entity.VatRatesEntity;
import vn.hoadon.repositories.VatRatesRepository;
import vn.hoadon.services.VatRatesService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class VatRatesServiceImpl implements VatRatesService {

    @Autowired
    private VatRatesRepository vatRatesRepository;

    @Override
    public VatRatesEntity create(VatRatesEntity taxRate) {
        return vatRatesRepository.save(taxRate);
    }

    @Override
    public List<VatRatesEntity> findAll() {
        return vatRatesRepository.findAll();
    }

    @Override
    public List<VatRatesEntity> findAllOrderedByPrioritize() {
        return vatRatesRepository.findAllByOrderByPrioritizeAsc();
    }

    @Override
    public VatRatesEntity findById(Integer id) {
        return vatRatesRepository.findById(id).orElse(null);
    }

    @Override
    public VatRatesEntity update(Integer id, VatRatesEntity taxRate) {
        VatRatesEntity existing = vatRatesRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }

        existing.setCode(taxRate.getCode());
        existing.setLabel(taxRate.getLabel());
        existing.setStatus(taxRate.getStatus());
        if (taxRate.getPrioritize() != null) {
            existing.setPrioritize(taxRate.getPrioritize());
        }
        existing.setUpdatedAt(taxRate.getUpdatedAt());

        return vatRatesRepository.save(existing);
    }

    @Override
    public void delete(Integer id) {
        vatRatesRepository.deleteById(id);
    }

    @Override
    public Page<VatRatesEntity> pageByUser(Integer userId, Integer status, Pageable pageable, String keyword) {
        Page<VatRatesEntity> page = (status == null)
                ? vatRatesRepository.findByUserId(userId, pageable)
                : vatRatesRepository.findByUserIdAndStatus(userId, status, pageable);

        if (keyword == null || keyword.isBlank()) {
            return page;
        }
        String kw = keyword.trim().toLowerCase();
        List<VatRatesEntity> filtered = page.getContent().stream()
                .filter(it -> {
                    String label = it.getLabel() != null ? it.getLabel().toLowerCase() : "";
                    String codeStr = it.getCode() != null ? it.getCode().toString() : "";
                    return label.contains(kw) || codeStr.contains(kw);
                }).collect(Collectors.toList());
        return new PageImpl<>(filtered, pageable, page.getTotalElements());
    }

    @Override
    public Page<VatRatesEntity> pageAll(Integer status, Pageable pageable, String keyword) {
        Page<VatRatesEntity> page = (status == null)
                ? vatRatesRepository.findAll(pageable)
                : vatRatesRepository.findByStatus(status, pageable);

        if (keyword == null || keyword.isBlank()) {
            return page;
        }
        String kw = keyword.trim().toLowerCase();
        List<VatRatesEntity> filtered = page.getContent().stream()
                .filter(it -> {
                    String label = it.getLabel() != null ? it.getLabel().toLowerCase() : "";
                    String codeStr = it.getCode() != null ? it.getCode().toString() : "";
                    return label.contains(kw) || codeStr.contains(kw);
                }).collect(Collectors.toList());
        return new PageImpl<>(filtered, pageable, page.getTotalElements());
    }

    @Override
    public void reorder(List<Integer> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            Integer id = orderedIds.get(i);
            VatRatesEntity entity = vatRatesRepository.findById(id).orElse(null);
            if (entity != null) {
                entity.setPrioritize(i);
                vatRatesRepository.save(entity);
            }
        }
    }
}