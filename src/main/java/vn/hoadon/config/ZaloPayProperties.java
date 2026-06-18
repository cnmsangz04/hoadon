package vn.hoadon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zalopay")
public class ZaloPayProperties {

    private boolean enabled = true;
    private String appId = "2553";
    private String key1 = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
    private String key2 = "kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz";
    private String createOrderUrl = "https://sb-openapi.zalopay.vn/v2/create";
    private String queryOrderUrl = "https://sb-openapi.zalopay.vn/v2/query";
    private String bankListUrl = "https://sbgateway.zalopay.vn/api/getlistmerchantbanks";
    private String bankListAppId = "2553";
    private String bankListKey1 = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
    private String redirectUrl = "";
    private String callbackUrl = "";
    private String appUser = "hoadon";
    private String bankCode = "";
    private String preferredPaymentMethods = "zalopay_wallet";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getKey1() { return key1; }
    public void setKey1(String key1) { this.key1 = key1; }

    public String getKey2() { return key2; }
    public void setKey2(String key2) { this.key2 = key2; }

    public String getCreateOrderUrl() { return createOrderUrl; }
    public void setCreateOrderUrl(String createOrderUrl) { this.createOrderUrl = createOrderUrl; }

    public String getQueryOrderUrl() { return queryOrderUrl; }
    public void setQueryOrderUrl(String queryOrderUrl) { this.queryOrderUrl = queryOrderUrl; }

    public String getBankListUrl() { return bankListUrl; }
    public void setBankListUrl(String bankListUrl) { this.bankListUrl = bankListUrl; }

    public String getBankListAppId() { return bankListAppId; }
    public void setBankListAppId(String bankListAppId) { this.bankListAppId = bankListAppId; }

    public String getBankListKey1() { return bankListKey1; }
    public void setBankListKey1(String bankListKey1) { this.bankListKey1 = bankListKey1; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    public String getAppUser() { return appUser; }
    public void setAppUser(String appUser) { this.appUser = appUser; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getPreferredPaymentMethods() { return preferredPaymentMethods; }
    public void setPreferredPaymentMethods(String preferredPaymentMethods) {
        this.preferredPaymentMethods = preferredPaymentMethods;
    }
}
