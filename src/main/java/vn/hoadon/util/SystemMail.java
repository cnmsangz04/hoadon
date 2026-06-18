package vn.hoadon.util;

import java.util.Set;

public final class SystemMail {

    public static final long COMPANY_ID = 1L;

    private static final Set<String> COMPANY_ONE_KEYS = Set.of(
            "ACCOUNT_INFO_MAIL",
            "LOGIN_INFO_MAIL",
            "RESET_PASSWORD_MAIL",
            "BUY_INVOICE_MAIL",
            "DAILY_INVOICE_REPORT_MAIL"
    );

    private SystemMail() {
    }

    public static boolean usesCompanyOne(String templateKey) {
        return templateKey != null && COMPANY_ONE_KEYS.contains(templateKey);
    }

    public static Long resolveCompanyId(String templateKey, Long fallbackCompanyId) {
        return usesCompanyOne(templateKey) ? COMPANY_ID : fallbackCompanyId;
    }
}
