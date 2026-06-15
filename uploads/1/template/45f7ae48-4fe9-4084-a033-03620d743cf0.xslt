<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
    <xsl:output method="html" encoding="UTF-8" indent="yes"/>
    
    <xsl:template match="/">
        <html>
        <head>
            <meta charset="UTF-8"/>
            <title>Hóa đơn GTGT</title>
            <style>
                @page {
                    size: A4;
                    margin: 0;
                }
                
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    font-family: 'Times New Roman', Times, serif;
                    font-size: 13px;
                    line-height: 1.3;
                    color: #000;
                    background: #fff;
                }
                
                .page {
                    width: 210mm;
                    min-height: 297mm;
                    padding: 10mm;
                    margin: 0 auto;
                    background: white;
                    position: relative;
                }
                
                /* VIỀN HOA VĂN */
                .border-frame {
                    position: absolute;
                    top: 8mm;
                    left: 8mm;
                    right: 8mm;
                    bottom: 8mm;
                    border: 2.5px solid #0099cc;
                    padding: 3mm;
                }
                
                .border-frame::before {
                    content: '';
                    position: absolute;
                    top: -5px;
                    left: -5px;
                    right: -5px;
                    bottom: -5px;
                }
                
                .content {
                    position: relative;
                    z-index: 1;
                    padding: 5mm;
                }
                
                /* HEADER */
                .header {
                    display: grid;
                    grid-template-columns: 80px 1fr auto;
                    gap: 10px;
                    padding-bottom: 8px;
                    border-bottom: 2px solid #000;
                    margin-bottom: 12px;
                }
                
                .logo {
                    width: 70px;
                    height: 70px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: white;
                }
                
                .company-info {
                    padding-top: 3px;
                }
                
                .company-name {
                    font-size: 14px;
                    font-weight: bold;
                    text-transform: uppercase;
                    margin-bottom: 4px;
                }
                
                .company-details {
                    font-size: 10.5px;
                    line-height: 1.4;
                }
                
                .company-details div {
                    margin-bottom: 1px;
                }
                
                .en {
                    font-style: italic;
                }
                
                /* QR CODE */
                .qr-code {
                    position: absolute;
                    left: 15mm;
                    top: 35mm;
                    width: 70px;
                    height: 70px;
                    border: 2px solid #000;
                    background: white;
                }
                
                /* TITLE */
                .title-section {
                    text-align: center;
                    margin: 15px 0 12px 0;
                    position: relative;
                }
                
                .main-title {
                    font-size: 16px;
                    font-weight: bold;
                    letter-spacing: 0.5px;
                    margin-bottom: 2px;
                }
                
                .sub-title {
                    font-size: 13px;
                    font-style: italic;
                    margin-bottom: 6px;
                }
                
                .date-line {
                    font-size: 11.5px;
                }
                
                .serial-box {
                    position: absolute;
                    right: 0;
                    top: 0;
                    text-align: right;
                    font-size: 11.5px;
                    line-height: 1.5;
                }
                
                .serial-box .number {
                    color: #e74c3c;
                    font-weight: bold;
                    font-size: 15px;
                }
                
                /* BUYER INFO */
                .buyer-section {
                    margin: 12px 0;
                    font-size: 11.5px;
                }

                /* Dòng 1 cặp label – value */
                .buyer-line {
                    display: table;
                    width: 100%;
                    margin-bottom: 2px;
                    min-height: 18px;
                }

                .buyer-line > div {
                    display: table-cell;
                    vertical-align: middle;
                }

                .buyer-label {
                    width: 200px;
                    font-weight: normal;
                    white-space: nowrap;
                }

                .buyer-value {
                    width: auto;
                    border-bottom: 1px dotted #666;
                    padding-left: 5px;
                    word-break: break-word;
                }

                /* Dòng 2 cặp (payment) */
                .payment-line {
                    display: table;
                    width: 100%;
                    margin-bottom: 2px;
                }

                .payment-line > div {
                    display: table-cell;
                    vertical-align: middle;
                }

                .payment-line .buyer-label {
                    width: 200px;
                    white-space: nowrap;
                    font-weight: normal;
                }

                .payment-line .buyer-value {
                    width: 25%;
                    border-bottom: 1px dotted #666;
                    padding-left: 5px;
                    word-break: break-word;
                }

                
                /* TABLE */
                .items-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 10px 0;
                    font-size: 10.5px;
                }
                
                .items-table th,
                .items-table td {
                    border: 1px solid #000;
                    padding: 4px 3px;
                    vertical-align: middle;
                }
                
                .items-table thead th {
                    background: white;
                    font-weight: bold;
                    text-align: center;
                    line-height: 1.3;
                }
                
                .items-table .col-label {
                    font-size: 9.5px;
                    line-height: 1.2;
                }
                
                .items-table .col-index {
                    font-size: 9px;
                    color: #666;
                    padding: 1px 0;
                }
                
                .items-table tbody td {
                    height: 24px;
                }
                
                .items-table .text-center {
                    text-align: center;
                }
                
                .items-table .text-right {
                    text-align: right;
                    padding-right: 5px;
                }
                
                .items-table .text-left {
                    text-align: left;
                    padding-left: 5px;
                }
                
                .total-row td {
                    font-weight: bold;
                    background: #f9f9f9;
                }
                
                /* AMOUNT IN WORDS */
                .amount-words {
                    margin: 8px 0;
                    font-size: 11.5px;
                    border-bottom: 1px dotted #666;
                    padding-bottom: 2px;
                }
                
                /* TAX AUTHORITY CODE */
                .tax-authority-section {
                    margin: 12px 0 8px 0;
                    font-size: 11px;
                    padding: 8px;
                    border: 1px dashed #999;
                    background: #f9f9f9;
                }
                
                .tax-authority-title {
                    font-weight: bold;
                    margin-bottom: 5px;
                    color: #333;
                }
                
                .tax-authority-code {
                    font-family: 'Courier New', monospace;
                    font-size: 10px;
                    color: #000;
                    padding: 5px;
                    background: white;
                    border: 1px solid #ccc;
                    word-break: break-all;
                }
                
                /* SIGNATURES */
                .signatures {
                    display: flex;
                    justify-content: space-around;
                    margin-top: 20px;
                    font-size: 11.5px;
                    text-align: center;
                }
                
                .sig-box {
                    width: 45%;
                }
                
                .sig-title {
                    font-weight: bold;
                    margin-bottom: 3px;
                }
                
                .sig-note {
                    font-style: italic;
                    font-size: 10.5px;
                    margin-bottom: 8px;
                }
                
                /* DIGITAL SIGNATURE */
                .digital-signature {
                    margin-top: 8px;
                    padding: 8px;
                    border: 1px solid #666;
                    background: #fafafa;
                    font-size: 9px;
                    text-align: left;
                }
                
                .digital-signature-title {
                    font-weight: bold;
                    margin-bottom: 5px;
                    text-align: center;
                    font-size: 10px;
                    color: #333;
                    text-decoration: underline;
                }
                
                .signature-info {
                    margin-bottom: 4px;
                    line-height: 1.4;
                }
                
                .signature-label {
                    font-weight: bold;
                    color: #555;
                }
                
                .signature-value {
                    font-family: 'Courier New', monospace;
                    color: #000;
                    word-break: break-all;
                    margin-left: 10px;
                    font-size: 8.5px;
                }
                
                .signature-time {
                    margin-top: 6px;
                    font-style: italic;
                    color: #666;
                }
                
                /* FOOTER */
                .footer {
                    margin-top: 20px;
                    text-align: center;
                    font-size: 10px;
                }
                
                .footer div {
                    margin-bottom: 1px;
                }
                
                @media print {
                    .page {
                        margin: 0;
                        box-shadow: none;
                    }
                }
            </style>
        </head>
        <body>
            <div class="page">
                <div class="border-frame">
                    <div class="content">
                        
                        <!-- HEADER -->
                        <div class="header">
                            <div class="logo">
                                <img style="width: 100px;" src="data:image/png;base64,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"/>
                            </div>
                            
                            <div class="company-info">
                                <div class="company-name">
                                    <xsl:value-of select="HDon/DLHDon/NDHDon/NBan/Ten"/>
                                </div>
                                <div class="company-details">
                                    <div>
                                        Địa chỉ <span class="en">(Address):</span> 
                                         <xsl:value-of select="HDon/DLHDon/NDHDon/NBan/DChi"/>
                                    </div>
                                    <div>
                                        Mã số thuế <span class="en">(Tax code):</span> 
                                        <b> <xsl:value-of select="HDon/DLHDon/NDHDon/NBan/MST"/></b>
                                    </div>
                                    <div>
                                        Điện thoại <span class="en">(Tel):</span> 
                                        <xsl:value-of select="HDon/DLHDon/NDHDon/NBan/SDThoai"/> 
                                         Email: <xsl:value-of select="HDon/DLHDon/NDHDon/NBan/DCTDTu"/>
                                    </div>
                                    <div>
                                        Số tài khoản <span class="en">(Bank Account):</span> 
                                         <xsl:value-of select="HDon/DLHDon/NDHDon/NBan/STKNHang"/> 
                                        tại <span class="en">(At):</span> 
                                         <xsl:value-of select="HDon/DLHDon/NDHDon/NBan/TNHang"/>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <!-- QR CODE -->
                        <div class="qr-code"></div>
                        
                        <!-- TITLE -->
                        <div class="title-section">
                            <div class="serial-box">
                                <div>
                                    Ký hiệu <span class="en">(Serial):</span> 
                                    <b> <xsl:value-of select="HDon/DLHDon/TTChung/KHMSHDon"/><xsl:value-of select="HDon/DLHDon/TTChung/KHHDon"/></b>
                                </div>
                                <div>
                                    Số <span class="en">(No):</span> 
                                    <span class="number"> <xsl:value-of select="HDon/DLHDon/TTChung/SHDon"/></span>
                                </div>
                            </div>
                            
                            <div class="main-title">HÓA ĐƠN GIÁ TRỊ GIA TĂNG</div>
                            <div class="sub-title">(VAT INVOICE)</div>
                            <div class="date-line">
                                Ngày <span class="en">(day)</span>   
                                <xsl:value-of select="substring(HDon/DLHDon/TTChung/NLap, 9, 2)"/> 
                                tháng <span class="en">(month)</span> 
                                <xsl:value-of select="substring(HDon/DLHDon/TTChung/NLap, 6, 2)"/> 
                                năm <span class="en">(year)</span> 
                                <xsl:value-of select="substring(HDon/DLHDon/TTChung/NLap, 1, 4)"/>
                            </div>
                        </div>
                        
                        <!-- BUYER INFO -->
                        <div class="buyer-section">

                            <div class="buyer-line">
                                <div class="buyer-label">
                                    Họ tên người mua hàng <span class="en">(Buyer):</span>
                                </div>
                                <div class="buyer-value">
                                    <xsl:value-of select="HDon/DLHDon/NDHDon/NMua/HVTNMHang"/>
                                </div>
                            </div>

                            <div class="buyer-line">
                                <div class="buyer-label">
                                    Tên đơn vị <span class="en">(Company's name):</span>
                                </div>
                                <div class="buyer-value">
                                    <xsl:value-of select="HDon/DLHDon/NDHDon/NMua/Ten"/>
                                </div>
                            </div>

                            <div class="buyer-line">
                                <div class="buyer-label">
                                    Mã số thuế <span class="en">(Tax code):</span>
                                </div>
                                <div class="buyer-value">
                                    <xsl:value-of select="HDon/DLHDon/NDHDon/NMua/MST"/>
                                </div>
                            </div>

                            <div class="buyer-line">
                                <div class="buyer-label">
                                    Địa chỉ <span class="en">(Address):</span>
                                </div>
                                <div class="buyer-value">
                                    <xsl:value-of select="HDon/DLHDon/NDHDon/NMua/DChi"/>
                                </div>
                            </div>

                            <div class="payment-line">
                                <div class="buyer-label">
                                    Thanh toán <span class="en">(Payment Method):</span>
                                </div>
                                <div class="buyer-value">
                                    <xsl:value-of select="HDon/DLHDon/TTChung/HTTToan"/>
                                </div>
                                <div class="buyer-label">
                                    Số tài khoản <span class="en">(Bank Account):</span>
                                </div>
                                <div class="buyer-value">
                                    <xsl:value-of select="HDon/DLHDon/NDHDon/NMua/STKNHang"/>
                                </div>
                            </div>

                            <xsl:if test="HDon/DLHDon/TTChung/TTHDLQuan/GChu != ''">
                                <div style="text-align: center; margin-top: 8px;">
                                    <xsl:value-of select="HDon/DLHDon/TTChung/TTHDLQuan/GChu"/>
                                </div>
                            </xsl:if>

                        </div>
                        
                        <!-- TABLE -->
                        <table class="items-table">
                            <thead>
                                <tr>
                                    <th style="width: 30px;">
                                        <div class="col-label">STT</div>
                                        <div class="col-index en">(No.)</div>
                                    </th>
                                    <th style="width: 28%;">
                                        <div class="col-label">Tên hàng hóa, dịch vụ</div>
                                        <div class="col-index en">(Goods, services, description)</div>
                                    </th>
                                    <th style="width: 45px;">
                                        <div class="col-label">Đơn vị tính</div>
                                        <div class="col-index en">(Unit)</div>
                                    </th>
                                    <th style="width: 55px;">
                                        <div class="col-label">Số lượng</div>
                                        <div class="col-index en">(Quantity)</div>
                                    </th>
                                    <th style="width: 75px;">
                                        <div class="col-label">Đơn giá</div>
                                        <div class="col-index en">(Price)</div>
                                    </th>
                                    <th style="width: 85px;">
                                        <div class="col-label">Thành tiền</div>
                                        <div class="col-index en">(Total)</div>
                                    </th>
                                    <th style="width: 45px;">
                                        <div class="col-label">Thuế suất</div>
                                        <div class="col-index en">(VAT rate)</div>
                                    </th>
                                    <th style="width: 75px;">
                                        <div class="col-label">Thuế GTGT</div>
                                        <div class="col-index en">(VAT amount)</div>
                                    </th>
                                    <th style="width: 85px;">
                                        <div class="col-label">Cộng</div>
                                        <div class="col-index en">(Amount)</div>
                                    </th>
                                </tr>
                                <tr>
                                    <th style="padding: 2px;">1</th>
                                    <th style="padding: 2px;">2</th>
                                    <th style="padding: 2px;">3</th>
                                    <th style="padding: 2px;">4</th>
                                    <th style="padding: 2px;">5</th>
                                    <th style="padding: 2px;">6=4x5</th>
                                    <th style="padding: 2px;">7</th>
                                    <th style="padding: 2px;">8=6x7</th>
                                    <th style="padding: 2px;">9=6+8</th>
                                </tr>
                            </thead>
                            <tbody>
                                <xsl:choose>
                                    <xsl:when test="HDon/DLHDon/NDHDon/DSHHDVu/HHDVu">
                                        <xsl:for-each select="HDon/DLHDon/NDHDon/DSHHDVu/HHDVu">
                                            <tr>
                                                <td class="text-center"><xsl:value-of select="position()"/></td>
                                                <td class="text-left"><xsl:value-of select="THHDVu"/></td>
                                                <td class="text-center"><xsl:value-of select="DVTinh"/></td>
                                                <td class="text-right"><xsl:value-of select="SLuong"/></td>
                                                <td class="text-right"><xsl:value-of select="format-number(DGia, '#,###')"/></td>
                                                <td class="text-right"><xsl:value-of select="format-number(ThTien, '#,###')"/></td>
                                                <td class="text-center"><xsl:value-of select="TSuat"/></td>
                                                <td class="text-right">
                                                    <xsl:for-each select="TTKhac/TTin">
                                                        <xsl:if test="TTruong = 'TThue'">
                                                            <xsl:choose>
                                                                <xsl:when test="DLieu != ''">
                                                                    <xsl:value-of select="format-number(DLieu, '#,###')"/>
                                                                </xsl:when>
                                                                <xsl:otherwise>
                                                                     
                                                                </xsl:otherwise>
                                                            </xsl:choose>
                                                        </xsl:if>
                                                    </xsl:for-each>
                                                </td>
                                                <td class="text-right">
                                                    <xsl:for-each select="TTKhac/TTin">
                                                        <xsl:if test="TTruong = 'TCong'">
                                                            <xsl:choose>
                                                                <xsl:when test="DLieu != ''">
                                                                    <xsl:value-of select="format-number(DLieu, '#,###')"/>
                                                                </xsl:when>
                                                                <xsl:otherwise>
                                                                     
                                                                </xsl:otherwise>
                                                            </xsl:choose>
                                                        </xsl:if>
                                                    </xsl:for-each>
                                                </td>
                                            </tr>
                                        </xsl:for-each>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <tr><td colspan="9" style="height: 25px;"></td></tr>
                                        <tr><td colspan="9" style="height: 25px;"></td></tr>
                                        <tr><td colspan="9" style="height: 25px;"></td></tr>
                                        <tr><td colspan="9" style="height: 25px;"></td></tr>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <tr class="total-row">
                                    <td colspan="5" class="text-left" style="padding-left: 5px;">
                                        Tổng cộng tiền thanh toán <span class="en">(Total amount):</span>
                                    </td>
                                    <td class="text-right">
                                        <xsl:if test="HDon/DLHDon/NDHDon/TToan/TgTCThue">
                                            <xsl:value-of select="format-number(HDon/DLHDon/NDHDon/TToan/TgTCThue, '#,###')"/>
                                        </xsl:if>
                                    </td>
                                    <td></td>
                                    <td class="text-right">
                                        <xsl:if test="HDon/DLHDon/NDHDon/TToan/TgTThue">
                                            <xsl:value-of select="format-number(HDon/DLHDon/NDHDon/TToan/TgTThue, '#,###')"/>
                                        </xsl:if>
                                    </td>
                                    <td class="text-right">
                                        <xsl:if test="HDon/DLHDon/NDHDon/TToan/TgTTTBSo">
                                            <xsl:value-of select="format-number(HDon/DLHDon/NDHDon/TToan/TgTTTBSo, '#,###')"/>
                                        </xsl:if>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        
                        <!-- AMOUNT IN WORDS -->
                        <div class="amount-words">
                            Số tiền viết bằng chữ <span class="en">(Amount in words):</span>
                            <b><xsl:value-of select="HDon/DLHDon/NDHDon/TToan/TgTTTBChu"/></b>
                        </div>
                        
                        <!-- TAX AUTHORITY CODE -->
                        <xsl:if test="HDon/MCCQT != ''">
                            <div class="tax-authority-section">
                                <div class="tax-authority-title">
                                    Mã của cơ quan thuế (Tax Authority Code):
                                </div>
                                <div class="tax-authority-code">
                                    <xsl:value-of select="HDon/MCCQT"/>
                                </div>
                            </div>
                        </xsl:if>
                        
                        <!-- SIGNATURES -->
                        <div class="signatures">
                            <div class="sig-box">
                                <div class="sig-title">
                                    Người mua hàng <span class="en">(Buyer)</span>
                                </div>
                                <div class="sig-note">
                                    ( Ký, ghi rõ họ tên )<br/>
                                    <span class="en">(Sign, full name)</span>
                                </div>
                            </div>
                            
                            <div class="sig-box">
                                <div class="sig-title">
                                    Người bán hàng <span class="en">(Seller)</span>
                                </div>
                                <div class="sig-note">
                                    ( Ký, ghi rõ họ tên )<br/>
                                    <span class="en">(Sign, full name)</span>
                                </div>
                                
                                <!-- DIGITAL SIGNATURE -->
                                <xsl:if test="HDon/DSCKS/NBan/ds:Signature">
                                    <div class="digital-signature">
                                        <div class="digital-signature-title">
                                            &#9989; CHỮ KÝ SỐ (DIGITAL SIGNATURE)
                                        </div>
                                        
                                        <xsl:if test="HDon/DSCKS/NBan/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509SubjectName">
                                            <div class="signature-info">
                                                <span class="signature-label">Người ký:</span>
                                                <span class="signature-value">
                                                    <xsl:value-of select="HDon/DLHDon/NDHDon/NBan/Ten"/>
                                                </span>
                                            </div>
                                        </xsl:if>
                                        
                                        <xsl:if test="HDon/DSCKS/NBan/ds:Signature/ds:Object/ds:SignatureProperties/ds:SignatureProperty/ds:SigningTime">
                                            <div class="signature-time">
                                                <span class="signature-label">Thời gian ký:</span>
                                                <span class="signature-value">
                                                    <xsl:value-of select="HDon/DSCKS/NBan/ds:Signature/ds:Object/ds:SignatureProperties/ds:SignatureProperty/ds:SigningTime"/>
                                                </span>
                                            </div>
                                        </xsl:if>

                                    </div>
                                </xsl:if>
                            </div>
                        </div>
                        
                        <!-- FOOTER -->
                        <div class="footer">
                            <div>Trang tra cứu: https://tracuu.hoadon.vn Mã tra cứu 
                                <b><xsl:for-each select="HDon/DLHDon/TTKhac/TTin">
                                    <xsl:if test="TTruong = 'MTCuu'">
                                        <xsl:choose>
                                            <xsl:when test="DLieu != ''">
                                                <xsl:value-of select="DLieu"/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                 
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:if>
                                </xsl:for-each></b></div>
                            <div><b>Giải Pháp Hóa Đơn Điện Tử</b></div>
                        </div>
                        
                    </div>
                </div>
            </div>
        </body>
        </html>
    </xsl:template>
</xsl:stylesheet>