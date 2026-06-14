package vn.hoadon.services;


import vn.hoadon.dto.mail.MailTemplateDto;
import vn.hoadon.entity.VatRatesEntity;

import java.util.List;

public interface MailTemplateService {

    /**
     * Lấy danh sách mẫu mail theo công ty
     */
    List<MailTemplateDto> getByCompanyId(Integer companyId);

    /**
     * Lấy mẫu mail theo id
     */
    MailTemplateDto getById(Integer id);

    /**
     * Thêm mới mẫu mail
     */
    MailTemplateDto create(MailTemplateDto dto);

    /**
     * Cập nhật mẫu mail
     */
    MailTemplateDto update(Integer id, MailTemplateDto dto);

    /**
     * Xóa mẫu mail
     */
    void delete(Integer id);
}
