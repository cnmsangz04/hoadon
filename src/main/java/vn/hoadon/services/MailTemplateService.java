package vn.hoadon.services;


import vn.hoadon.dto.mail.MailTemplateDto;
import vn.hoadon.entity.VatRatesEntity;

import java.util.List;

public interface MailTemplateService {

    /**
     * Lấy danh sách mail template theo company
     */
    List<MailTemplateDto> getByCompanyId(Integer companyId);

    /**
     * Lấy mail template theo id
     */
    MailTemplateDto getById(Integer id);

    /**
     * Thêm mới mail template
     */
    MailTemplateDto create(MailTemplateDto dto);

    /**
     * Cập nhật mail template
     */
    MailTemplateDto update(Integer id, MailTemplateDto dto);

    /**
     * Xóa mail template
     */
    void delete(Integer id);
}
