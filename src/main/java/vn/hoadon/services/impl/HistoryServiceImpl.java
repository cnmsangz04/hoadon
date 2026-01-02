package vn.hoadon.services.impl;

import org.springframework.stereotype.Service;
import vn.hoadon.dto.history.HistoryDto;
import vn.hoadon.entity.HistoryEntity;
import vn.hoadon.repositories.HistoryRepository;
import vn.hoadon.services.HistoryService;

@Service
public class HistoryServiceImpl implements HistoryService {

    private final HistoryRepository historyRepository;

    public HistoryServiceImpl(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
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
}
