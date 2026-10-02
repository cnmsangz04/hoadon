<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ex="http://exslt.org/dates-and-times" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" version="1.0" extension-element-prefixes="ex" exclude-result-prefixes="ex ds">
    <xsl:output method="xml" encoding="UTF-8" omit-xml-declaration="yes"/>
    <xsl:param name="status" select="0"/>
    <xsl:template match="TBao">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
                <title>Thông báo hóa đơn điện tử sai sót</title>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <meta http-equiv="X-UA-Compatible" content="IE=Edge"/>
                <style>
                    @media print {
                    .paginate{
                    display: none;
                    }

                    }
                </style>
            </head>
            <body>
                <div class="main" data-name="main" style="font-family: serif; font-size: 14px; font-weight: normal; box-sizing: border-box;margin: 0px auto;width:100%;padding: 15pt;position: relative">
                    <table cellpadding="0" cellspacing="0" border="0" class="tblmain" style="font-family: serif; font-size: 14px; font-weight: normal; box-sizing: border-box; width: 100%;">
                        <thead style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                            <tr class="hidden" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0;">
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                                <th width="2%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 2%;">
                                     
                                </th>
                            </tr>
                            <tr class="break" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px;">
                                <td colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <tr class="break" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box;">
                                <td align="right" colspan="50" style="font-family: 'serif'; font-weight: bold; box-sizing: border-box;vertical-align: top; min-width: 5%;">
                                    <span class="label" style="font-family: 'serif'; font-size: 14px; font-weight: bold;">
                                        Mẫu số:
                                    </span>
                                    <xsl:value-of select="DLTBao/MSo"/>
                                </td>
                            </tr>
                            <tr class="break" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px;">
                                <td colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                <td align="center" colspan="50" style="font-family: 'serif';font-weight: bold; box-sizing: border-box; vertical-align: top; min-width: 5%;">
                                    <h4 style="font-family: 'serif';margin:5px 0px;font-size: 17px;">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</h4>
                                    <p  style="font-family: 'serif';margin:5px 0px;font-size: 14px;">Độc lập - Tự do - Hạnh phúc</p>
                                    <p  style="font-family: 'serif';margin:5px 0px;font-size: 14px;">--------o0o--------</p>
                                </td>
                            </tr>
                            <tr class="break" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px;">
                                <td colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                <td align="center" colspan="50" style="font-family: 'serif'; font-size: 17px; font-weight: normal; box-sizing: border-box; vertical-align: top; min-width: 5%;">
                                    <h3 style="font-family: 'serif';margin:5px 0px">THÔNG BÁO HÓA ĐƠN ĐIỆN TỬ CÓ SAI SÓT</h3>
                                </td>
                            </tr>
                        </thead>
                        <tbody style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                            <tr class="break" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px;">
                                <td colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <tr class="break" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px;">
                                <td colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                <td colspan="50" class="dotted" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; border-bottom: 1px dotted #0c0c0d; vertical-align: top; min-width: 5%;">
                                    <span class="label" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; display: inline-block; border-bottom: 3px solid #fff; border-right: 5px solid #fff; margin-bottom: -3px;">
                                        Kính gửi: (Cơ quan thuế):
                                    </span>
                                    <xsl:value-of select="DLTBao/TCQT"/>
                                </td>
                            </tr>
                            <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                <td class="spacing" colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 8px; font-size: 4px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                <td colspan="50" class="dotted" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; border-bottom: 1px dotted #0c0c0d; vertical-align: top; min-width: 5%;">
                                    <span class="label" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; display: inline-block; border-bottom: 3px solid #fff; border-right: 5px solid #fff; margin-bottom: -3px;">
                                        Tên người nộp thuế:
                                    </span>
                                    <xsl:value-of select="DLTBao/TNNT"/>
                                </td>
                            </tr>
                            <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                <td class="spacing" colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 8px; font-size: 4px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                <td colspan="50" class="dotted" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; border-bottom: 1px dotted #0c0c0d; vertical-align: top; min-width: 5%;">
                                    <span class="label" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; display: inline-block; border-bottom: 3px solid #fff; border-right: 5px solid #fff; margin-bottom: -3px;">
                                        Mã số thuế:
                                    </span>
                                    <xsl:value-of select="DLTBao/MST"/>
                                </td>
                            </tr>
                            <tr class="break" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px;">
                                <td colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px; vertical-align: top;">
                                     
                                </td>
                            </tr>
                            <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                <td colspan="50" align="left" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%;vertical-align: top">
                                    <span>
                                        Người nộp thuế thông báo về việc hóa đơn điện tử có sai sót như sau:
                                    </span>
                                </td>
                            </tr>
                            <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                <td colspan="50" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; vertical-align: top; min-width: 5%;">
                                    <table cellspacing="0" cellpadding="3" data-name="product" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; width: 100%; border-collapse: collapse;">
                                        <thead style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                            <tr class="hidden" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0;">
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                                <th width="5%" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; visibility: hidden; max-height: 0; font-size: 1px; border: 0; vertical-align: top; min-width: 5%;">
                                                     
                                                </th>
                                            </tr>
                                        </thead>
                                        <tbody style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; border: 1px solid #ccc;">
                                            <tr class="bold" style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; font-weight: bold; border: 1px solid #4a4a4a;">
                                                <td colspan="1" align="center" style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; font-weight: bold; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;">
                                                    STT
                                                </td>
                                                <td colspan="4" align="center" style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; font-weight: bold; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;">
                                                    Mã CQT cấp
                                                </td>
                                                <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; font-weight: bold; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;">
                                                    Ký hiệu mẫu hóa đơn và ký hiệu hóa đơn
                                                </td>
                                                <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; font-weight: bold; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;">
                                                    Số hóa đơn điện tử
                                                </td>
                                                <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; font-weight: bold; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;">
                                                    Ngày lập hóa đơn
                                                </td>
                                                <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; font-weight: bold; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;">
                                                    Loại áp dụng hóa đơn điện tử
                                                </td>
                                                <!-- <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; font-weight: bold; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;">
                                                    Mới / Hủy / Điều chỉnh / Thay thế / Giải trình
                                                </td> -->
                                                <td colspan="4" align="center" style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; font-weight: bold; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;">
                                                    Lý do
                                                </td>
                                            </tr>
                                            <tr style="font-family: 'Arial'; font-size: 14px; font-weight: normal; box-sizing: border-box; border: 1px solid #4a4a4a;">
                                                <td colspan="1" align="center"
                                                    style="font-family: 'Arial'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;padding: 5px 0">
                                                    1
                                                </td>
                                                <td colspan="4" align="center"
                                                    style="font-family: 'Arial'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;padding: 5px 0">
                                                    2
                                                </td>
                                                <td colspan="3" align="center"
                                                    style="font-family: 'Arial'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;padding: 5px 0">
                                                    3
                                                </td>
                                                <td colspan="3" align="center"
                                                    style="font-family: 'Arial'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;padding: 5px 0">
                                                    4
                                                </td>
                                                <td colspan="3" align="center"
                                                    style="font-family: 'Arial'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;padding: 5px 0">
                                                    5
                                                </td>
                                                <td colspan="3" align="center"
                                                    style="font-family: 'Arial'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;padding: 5px 0">
                                                    6
                                                </td>
                                                <!-- <td colspan="3" align="center"
                                                    style="font-family: 'Arial'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;padding: 5px 0">
                                                    7
                                                </td> -->
                                                <td colspan="4" align="center"
                                                    style="font-family: 'Arial'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;padding: 5px 0">
                                                    7
                                                </td>
                                            </tr>
                                            <xsl:for-each select="DLTBao/DSHDon/HDon">
                                                <xsl:sort select="@index" data-type="number"/>
                                                <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                                    <td colspan="1" align="center" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;border-bottom: 1px solid #4a4a4a;">
                                                        <xsl:value-of select="STT"/>
                                                    </td>
                                                    <td colspan="4" align="left" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;border-bottom: 1px solid #4a4a4a;">
                                                        <xsl:if test="MCCQT != ''">
                                                            <xsl:value-of select="MCCQT"/>
                                                        </xsl:if>
                                                        <xsl:if test="MCQTCap != ''">
                                                            <xsl:value-of select="MCQTCap"/>
                                                        </xsl:if>
                                                    </td>
                                                    <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;border-bottom: 1px solid #4a4a4a;">
                                                        <xsl:choose>
                                                            <xsl:when test="LADHDDT = 1 or LADHDDT = 5">
                                                               <xsl:value-of select="KHMSHDon"/><xsl:value-of select="KHHDon"/>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:value-of select="KHMSHDon"/><br/>
                                                                <xsl:value-of select="KHHDon"/>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </td>
                                                    <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;border-bottom: 1px solid #4a4a4a;">
                                                        <xsl:value-of select="format-number(SHDon, '00000000')" />
                                                    </td>
                                                    <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;border-bottom: 1px solid #4a4a4a;">
                                                        <span style="white-space:nowrap">
                                                            <xsl:variable name="DateExport" select="Ngay"/>
                                                            <xsl:value-of select="substring($DateExport, 9, 2)"/>/<xsl:value-of select="substring($DateExport, 6, 2)"/>/<xsl:value-of select="substring($DateExport, 1, 4)"/>
                                                        </span>
                                                    </td>
                                                    <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;border-bottom: 1px solid #4a4a4a;">
                                                        <xsl:choose>
                                                            <xsl:when test="LADHDDT = 1 and translate(../../NTBao, '-', '') &lt; 20260701">
                                                                Hóa đơn điện tử theo Nghị định 123/2020/NĐ-CP, Nghị định 70/2025/NĐ-CP
                                                            </xsl:when>
                                                            <xsl:when test="LADHDDT = 1">
                                                                Hóa đơn điện tử theo Nghị định 254/2026/NĐ-CP
                                                            </xsl:when>
                                                            <xsl:when test="LADHDDT = 2">
                                                                Hóa đơn có mã xác thực của CQT theo Nghị định số 51/2010/NĐ-CP và Nghị định số 04/2014/NĐ-CP
                                                            </xsl:when>
                                                            <xsl:when test="LADHDDT = 3">
                                                                Các loại hóa đơn theo Nghị định số 51/2010/NĐ-CP và Nghị định số 04/2014/NĐ-CP (Trừ hóa đơn điện tử có mã xác thực)
                                                            </xsl:when>
                                                            <xsl:when test="LADHDDT = 4 and translate(../../NTBao, '-', '') &lt; 20260701">
                                                                Hóa đơn đặt in theo Nghị định 123/2020/NĐ-CP
                                                            </xsl:when>
                                                            <xsl:when test="LADHDDT = 4">
                                                                Hóa đơn đặt in theo Nghị định 254/2026/NĐ-CP
                                                            </xsl:when>
                                                            <xsl:when test="LADHDDT = 5">
                                                                Hóa đơn đặt in theo Nghị định 254/2026/NĐ-CP
                                                            </xsl:when>
                                                        </xsl:choose>
                                                    </td>
                                                    <!-- <td colspan="3" align="center" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;border-bottom: 1px solid #4a4a4a;">
                                                        <xsl:choose>
                                                            <xsl:when test="TCTBao = 0">
                                                                Hóa đơn mới
                                                            </xsl:when>
                                                            <xsl:when test="TCTBao = 1">
                                                                Hóa đơn hủy
                                                            </xsl:when>
                                                            <xsl:when test="TCTBao = 2">
                                                                Điều chỉnh
                                                            </xsl:when>
                                                            <xsl:when test="TCTBao = 3">
                                                                Thay thế
                                                            </xsl:when>
                                                            <xsl:when test="TCTBao = 4">
                                                                Giải trình
                                                            </xsl:when>
                                                            <xsl:when test="TCTBao = 5">
                                                                Tổng hợp
                                                            </xsl:when>
                                                        </xsl:choose>
                                                    </td> -->
                                                    <td colspan="4" align="center" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; min-width: 5%; vertical-align: middle; border: 1px dotted #4a4a4a; border-left: 1px solid #4a4a4a; border-right: 1px solid #4a4a4a;border-bottom: 1px solid #4a4a4a;">
                                                        <xsl:value-of select="LDo"/>
                                                    </td>
                                                </tr>
                                            </xsl:for-each>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                            <!--Phan trang-->
                            <tr class="break" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px;">
                                <td colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <tr class="break" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px;">
                                <td colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 12px; font-size: 9px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <xsl:if test="not(paginate/@total) or paginate/@total = paginate/@current">
                                <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                    <td colspan="25" align="left" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; vertical-align: top; min-width: 5%;">
                                        &#160;
                                    </td>
                                    <td colspan="25" align="center" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; vertical-align: top; min-width: 5%;">
                                        <p style="font-family: 'serif'; font-size: 14px;padding: 5px 0;margin:0px">
                                            <xsl:variable name="NTBao" select="DLTBao/NTBao"/>
                                            <xsl:value-of select="DLTBao/DDanh"/>, ngày <xsl:value-of select="substring($NTBao, 9, 2)"/>/<xsl:value-of select="substring($NTBao, 6, 2)"/>/<xsl:value-of select="substring($NTBao, 1, 4)"/>
                                        </p>
                                        <br style="margin:5px 0px"/>
                                        <p style="font-family: 'serif'; font-size: 14px; box-sizing: border-box; padding: 5px 0;text-transform:uppercase;font-weight: bold;margin:0px">
                                            Người nộp thuế
                                        </p>
                                        <br style="margin:5px 0px"/>
                                        <span style="font-size: 13px; font-style: italic">
                                            (Chữ ký số, chữ ký điện tử người nộp thuế)
                                        </span>
                                    </td>
                                </tr>
                                <tr style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box;">
                                    <td colspan="25"> </td>
                                    <td colspan="25" align="center" style="font-family: 'serif'; font-size: 14px; font-weight: normal; box-sizing: border-box; vertical-align: top; min-width: 5%;">
                                        <xsl:choose>
                                            <xsl:when test="$status > 0">
                                                <table style="color: #fb0000; width:65%; margin-top: 10px;border: 1px solid #19476f;background: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAABbFJREFUaN7tmdtTU1cUxnlpvSJ4qdapY8eOnbZ2KhMcUQYsXiEkJNxBIwiIWlAETEhCEuQmyEVRBBRFCoiC4IUqolar1VK1Oq0Pfev0n/m61sGcekIuJxyFdiYPvzln77X3Ot+Xs7L3JgQBCPo/ExQwEDAQMICgruf7ZowdhR+4YxWxxkNM5L9qwJFavhgZFctA95WyDFx4lj9juIgqTyHxV/8qwLW/DyLVtoRNOHwa6BzPmzEk4q2L0P17Lvr/LBDo+SMP3McxrwbOPc2ZMZziky0LcfHVHvS93o/O8VwBvue+RHMomzB6NNDxJHtGOPvLHhZiTiLxXS+z6BPPB/WBYgJ83/t6HzpfGJBYFsImitwaaHu0e9ppf5wliGdhnc92oftVHqgPFJPQ8XMWxfai42k6dEeC2UThJAOtD3dNK2d+MrAAI4vvGM9E1285oD5QzC1tjwzCmNbHKUiYMJEvMXDqx4xp4/SDTEG8nsSfeZKK88+zQH2gmFdaH2bi/LMsHL+lgaZ4LpsIEQ2cvJs2LbTcS2fxJXrTArQ+Ssa5cQNO3U8HxWRTdzOBvg/Cl3q1aKDpTvJ7p3ksRRR/8oEO7U8zceJuCigmm9rr8Ug0hYp7g2ig4ZbeLY239fzQlUQDX7ntaaw3Gm8nCuJ1xmA039PgzOM0NI0mgmKyqR6Khd64QLKxiQbqbmonUT+i5UHauEOzYPpe5ay7fO53N94T9SMJnKco4ch8NIyp0fIgCcd/SKBS0Mrm6OB26FzESwzUXFdLqL2h5gF6Fu8YikLnS6q90W3QlAgmDnDcdY47am/Ei+Lrbm1H830djt2MB8Vk47iyxa14iYGqoR0i1cM7BPHqotmwDUTSK6dPYUSNprsaVN+IEU1UD8dK5rnCcRavLZ2H2pEtaBzToOZaLCgmG9ulb51rv8PrUaJiYOsEg1s5kBZXNAvW/g1ouKNG5dA2UEy41o/GofLaJrAoFnd0cNu/c9+C+3nD4XFVZJrnHb06kUculp4o55rv+zBn648Jsl2OeUt8BI7RK3cMbAbFRLhdO7IdFVRWThN2mifMf4N9Ik8hvykuPx5vvyLN4wvTxY1IKA32eIibZMDSF8UdBhZf1hdOpbIZ5f3RsPRGTcJ6KZreQgzsgxudJkqsNJ9izjwHWLxtYAOqrscI493l8UTphfWUd75P8RIDgnhebXpVqBjeBHNvJEzdGz1S1hMJx9VolF+OcJowUp8gPr54DsyX1tFbihbGecvjSvG5cNniXQ38WnBqDcq6I2DqisCR8+t9YqRxtsuRsJDYNybMLN7Uo6KSiZSdx0lRWxi0JfNki3c1kJtmX4riDhWKz4bLpqRzHSx9ETD3qZBsDYGxO0xol1K/P3kKW7+h1c0/8RIDh9rWckd7inUx6N4vitrXknAVlUs4lYEKhzvC/Jr/XctXiJ/YJP0SLzFQcPprATaRbFkEup8W9p34AvGH50xJvMTA/pNfirCJJPNCITm13xt5jasViZcYyG/6XAIFWxNNIdhLD6H2OyenfpVi8RIDuQ2fTYJN8Bkk5/gqUPudkX1sJdSHZwvi3zxHuYHsuk/dwib4IJZFD6W2Ygw1K8BnLF5y33qGcgO7a1Z4hAa28DpvqP4E1J4yOyuXi+Jd8is3sLNquVfYBB8PMis/BrX9JqNiKfiYwuLd5FZuIKNimU/YBO+06Y6lwu+WckmzfwQ+prB4D3mVG0izL5GFYIJWj1TbYhK2xCcp5YtE8V5yKjdAD5INTWzmWk62LhQEeiLJEorYQx8K4n3kU24gyRzqF/xHPtc0/zBF7UnwHhJ7cEK8jFzKDejLFviNYILKQ2cKBrVFuO0ULzOPcgM64/wpQUnq4qhMeK+gNl3nieL9yKHcgLZ07pRhEyxaQyuUU7yf85Ub0JTMUQQlyyZe8HUKc5UbCPybNWAgYCBgYMb4BxtYI2bs7SC4AAAAAElFTkSuQmCC') center center no-repeat">
                                                    <tr>
                                                        <td align="left" style="font-size: 12px;padding: 3px 2px;">
                                                            Signature Valid
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td align="left" style="font-size: 12px;padding: 3px 2px;">
                                                            Ký bởi:
                                                            <xsl:value-of select="DLTBao/TNNT"/>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td align="left" style="font-size: 12px; padding: 3px 2px;">
                                                            Ký ngày:
                                                            <xsl:variable name="SigningTime" select="DSCKS/NNT/ds:Signature/ds:Object/ds:SignatureProperties/ds:SignatureProperty/ds:SigningTime"/>
                                                            <span>
                                                                <xsl:value-of select="substring($SigningTime, 9, 2)"/> /
                                                                <xsl:value-of select="substring($SigningTime, 6, 2)"/> /
                                                                <xsl:value-of select="substring($SigningTime, 1, 4)"/>
                                                            </span>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <table style="height: 100px;">
                                                    <tr>
                                                        <td colspan="50">
                                                             
                                                        </td>
                                                    </tr>
                                                </table>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </td>
                                    <td colspan="2"> </td>
                                </tr>
                            </xsl:if>
                            <tr>
                                <td colspan="50" style="font-family: 'serif'; font-weight: normal; box-sizing: border-box; height: 1px; font-size: 9px; vertical-align: top; min-width: 5%;">
                                     
                                </td>
                            </tr>
                            <xsl:if test="paginate/@total">
                                <tr>
                                    <td colspan="50" align="left">
                                        <div class="paginate">
                                            <xsl:if test="paginate/@current &gt; 1">
                                                <a href="?page=1" style="display: inline-block;border: 1px solid #ccc;font-size: 13px;color: #333;border-radius: 3px;text-decoration: none;height: 20px;width: 20px;text-align: center;line-height: 20px;margin: 0 3px;">
                                                    &lt;&lt;
                                                </a>
                                                <a href="?page={paginate/@current -1}" style="display: inline-block;border: 1px solid #ccc;font-size: 13px;color: #333;border-radius: 3px;text-decoration: none;height: 20px;width: 20px;text-align: center;line-height: 20px;margin: 0 3px;">
                                                    &lt;
                                                </a>
                                            </xsl:if>
                                            <xsl:call-template name="page-button">
                                                <xsl:with-param name="num-page" select="0"/>
                                            </xsl:call-template>
                                            <xsl:if test="paginate/@current &lt; paginate/@total">
                                                <a href="?page={paginate/@current + 1}" style="display: inline-block;border: 1px solid #ccc;font-size: 13px;color: #333;border-radius: 3px;text-decoration: none;height: 20px;width: 20px;text-align: center;line-height: 20px;margin: 0 3px;">
                                                    &gt;
                                                </a>
                                                <a href="?page={paginate/@total}" style="display: inline-block;border: 1px solid #ccc;font-size: 13px;color: #333;border-radius: 3px;text-decoration: none;height: 20px;width: 20px;text-align: center;line-height: 20px;margin: 0 3px;">
                                                    &gt;&gt;
                                                </a>
                                            </xsl:if>
                                        </div>
                                    </td>
                                </tr>
                                <tr class="break">
                                    <td colspan="50" style="font-weight: normal; box-sizing: border-box; height: 1px; vertical-align: top;">
                                         
                                    </td>
                                </tr>
                            </xsl:if>
                        </tbody>
                    </table>
                </div>
            </body>
        </html>
    </xsl:template>
    <xsl:template name="page-button">
        <xsl:param name="num-page" select="0"/>
        <xsl:variable name="active">
            <xsl:if test="paginate/@current = $num-page + 1">background: #f58220;color: #fff;</xsl:if>
        </xsl:variable>

        <xsl:if test="$num-page &lt; paginate/@total">
            <a href="?page={$num-page + 1}" style="display: inline-block;border: 1px solid #ccc;font-size: 13px;color: #333;border-radius: 3px;text-decoration: none;height: 20px;width: 20px;text-align: center;line-height: 20px;margin: 0 3px;{$active}">
                <xsl:value-of select="$num-page + 1"/>
            </a>
            <xsl:call-template name="page-button">
                <xsl:with-param name="num-page" select="$num-page + 1"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    <xsl:decimal-format name="number" decimal-separator="," grouping-separator="."/>
</xsl:stylesheet>
