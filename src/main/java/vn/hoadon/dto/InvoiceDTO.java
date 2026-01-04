package vn.hoadon.dto;

import java.time.LocalDate;

public class InvoiceDTO {
    public Long id;
    public String formCode;
    public String serial;
    public Integer no;
    public LocalDate dateExport;
    public String lookupCode;
    public String customerName;
    public String codeCqt;
    public Double amount;
    public Long userId;
    public String username;
    public Short status;
}
