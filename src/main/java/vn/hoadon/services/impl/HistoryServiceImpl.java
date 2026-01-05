package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import vn.hoadon.dto.history.HistoryDto;
import vn.hoadon.entity.HistoryEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.HistoryRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.HistoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HistoryServiceImpl implements HistoryService {

    private final HistoryRepository historyRepository;
    private final UserRepository userRepository;

    public HistoryServiceImpl(HistoryRepository historyRepository, UserRepository userRepository) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Override
    public HistoryDto save(HistoryDto dto) {
        if (dto == null) return null;
        HistoryEntity e = new HistoryEntity();
        e.setCompanyId(dto.getCompanyId());
        e.setUserId(dto.getUserId());
        e.setTableName(dto.getTableName());
        e.setTableId(dto.getTableId());
        e.setTitle(dto.getTitle());
        e.setDescription(dto.getDescription());
        e.setShowNotify(dto.getShowNotify());
        e.setStatus(dto.getStatus());
        e.setType(dto.getType());
        e.setXmlData(dto.getXmlData());
        HistoryEntity saved = historyRepository.save(e);
        HistoryDto out = new HistoryDto();
        out.setId(saved.getId());
        out.setCompanyId(saved.getCompanyId());
        out.setUserId(saved.getUserId());
        out.setTableName(saved.getTableName());
        out.setTableId(saved.getTableId());
        out.setTitle(saved.getTitle());
        out.setDescription(saved.getDescription());
        out.setShowNotify(saved.getShowNotify());
        out.setStatus(saved.getStatus());
        out.setType(saved.getType());
        out.setXmlData(saved.getXmlData());
        out.setCreatedAt(saved.getCreatedAt());
        out.setUpdatedAt(saved.getUpdatedAt());
        return out;
    }

    @Override
    public List<HistoryDto> listByRegisterInvoice(Long companyId, Long registerInvoiceId) {
        if (companyId == null || registerInvoiceId == null) return List.of();
        List<HistoryEntity> all = historyRepository.findAll();
        List<HistoryEntity> filtered = new ArrayList<>();
        for (HistoryEntity h : all) {
            if (h == null) continue;
            if (companyId.equals(h.getCompanyId()) && "register_invoices".equals(h.getTableName()) && registerInvoiceId.equals(h.getTableId()) && h.getStatus() != null && h.getStatus() == 1 ) {
                filtered.add(h);
            }
        }
        // Collect userIds and fetch usernames
        Map<Long, String> userMap = userRepository.findAllById(
                filtered.stream().map(HistoryEntity::getUserId).filter(id -> id != null && id > 0).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(UserEntity::getId, u -> u.getUsername() != null ? u.getUsername() : ""));

        return filtered.stream().map(h -> {
            HistoryDto dto = new HistoryDto();
            dto.setId(h.getId());
            dto.setCompanyId(h.getCompanyId());
            dto.setUserId(h.getUserId());
            dto.setTableName(h.getTableName());
            dto.setTableId(h.getTableId());
            dto.setTitle(h.getTitle());
            dto.setDescription(h.getDescription());
            dto.setShowNotify(h.getShowNotify());
            dto.setStatus(h.getStatus());
            dto.setType(h.getType());
            dto.setXmlData(h.getXmlData());
            dto.setCreatedAt(h.getCreatedAt());
            dto.setUpdatedAt(h.getUpdatedAt());
            // Enrich username
            try { dto.setUsername(userMap.getOrDefault(h.getUserId(), null)); } catch (Exception ignored) {}
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<HistoryDto> listByInvoice(Long companyId, Long invoiceId) {
        if (companyId == null || invoiceId == null) return List.of();
        List<HistoryEntity> all = historyRepository.findAll();
        List<HistoryEntity> filtered = new ArrayList<>();
        for (HistoryEntity h : all) {
            if (h == null) continue;
            if (companyId.equals(h.getCompanyId()) && "invoices".equals(h.getTableName()) && invoiceId.equals(h.getTableId()) && h.getStatus() != null && h.getStatus() == 1 ) {
                filtered.add(h);
            }
        }
        // Sort by createdAt descending
        filtered.sort((a, b) -> {
            java.time.LocalDateTime ca = a.getCreatedAt();
            java.time.LocalDateTime cb = b.getCreatedAt();
            if (ca == null && cb == null) return 0;
            if (ca == null) return 1;
            if (cb == null) return -1;
            return cb.compareTo(ca);
        });
        // Collect userIds and fetch usernames
        Map<Long, String> userMap = userRepository.findAllById(
                filtered.stream().map(HistoryEntity::getUserId).filter(id -> id != null && id > 0).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(UserEntity::getId, u -> u.getUsername() != null ? u.getUsername() : ""));

        return filtered.stream().map(h -> {
            HistoryDto dto = new HistoryDto();
            dto.setId(h.getId());
            dto.setCompanyId(h.getCompanyId());
            dto.setUserId(h.getUserId());
            dto.setTableName(h.getTableName());
            dto.setTableId(h.getTableId());
            dto.setTitle(h.getTitle());
            dto.setDescription(h.getDescription());
            dto.setShowNotify(h.getShowNotify());
            dto.setStatus(h.getStatus());
            dto.setType(h.getType());
            dto.setXmlData(h.getXmlData());
            dto.setCreatedAt(h.getCreatedAt());
            dto.setUpdatedAt(h.getUpdatedAt());
            // Enrich username
            try { dto.setUsername(userMap.getOrDefault(h.getUserId(), null)); } catch (Exception ignored) {}
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<HistoryDto> listRecentByCompany(Long companyId, int limit) {
        if (companyId == null) return List.of();
        List<HistoryEntity> all = historyRepository.findAll();
        List<HistoryEntity> filtered = all.stream()
                .filter(h -> h != null && companyId.equals(h.getCompanyId()))
                .sorted((a, b) -> {
                    java.time.LocalDateTime ca = a.getCreatedAt();
                    java.time.LocalDateTime cb = b.getCreatedAt();
                    if (ca == null && cb == null) return 0;
                    if (ca == null) return 1;
                    if (cb == null) return -1;
                    return cb.compareTo(ca);
                })
                .limit(Math.max(1, limit))
                .collect(java.util.stream.Collectors.toList());
        java.util.Set<Long> uids = filtered.stream().map(HistoryEntity::getUserId).filter(id -> id != null && id > 0).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> userMap = userRepository.findAllById(uids).stream().collect(Collectors.toMap(UserEntity::getId, u -> u.getUsername() != null ? u.getUsername() : ""));
        return filtered.stream().map(h -> {
            HistoryDto dto = new HistoryDto();
            dto.setId(h.getId());
            dto.setCompanyId(h.getCompanyId());
            dto.setUserId(h.getUserId());
            dto.setTableName(h.getTableName());
            dto.setTableId(h.getTableId());
            dto.setTitle(h.getTitle());
            dto.setDescription(h.getDescription());
            dto.setShowNotify(h.getShowNotify());
            dto.setStatus(h.getStatus());
            dto.setType(h.getType());
            dto.setXmlData(h.getXmlData());
            dto.setCreatedAt(h.getCreatedAt());
            dto.setUpdatedAt(h.getUpdatedAt());
            dto.setUsername(userMap.getOrDefault(h.getUserId(), null));
            return dto;
        }).collect(Collectors.toList());
    }
}