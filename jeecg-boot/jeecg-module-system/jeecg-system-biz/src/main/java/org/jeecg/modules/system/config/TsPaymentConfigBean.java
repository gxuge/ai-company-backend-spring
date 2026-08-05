package org.jeecg.modules.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三方支付配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = TsPaymentConfigBean.PREFIX)
public class TsPaymentConfigBean {

    public static final String PREFIX = "jeecg.payment";

    /** 默认支付币种。 */
    private String defaultCurrency = "USD";
    /** HTTP 连接超时毫秒数。 */
    private int connectTimeoutMs = 5000;
    /** HTTP 读取超时毫秒数。 */
    private int readTimeoutMs = 30000;
    /** Stripe 配置。 */
    private Stripe stripe = new Stripe();
    /** PayPal 配置。 */
    private Paypal paypal = new Paypal();

    /**
     * Stripe 配置。
     */
    @Data
    public static class Stripe {
        /** 是否启用。 */
        private boolean enabled;
        /** Stripe Secret Key。 */
        private String secretKey;
        /** Stripe Webhook Secret。 */
        private String webhookSecret;
        /** Stripe API 根地址。 */
        private String apiBaseUrl = "https://api.stripe.com";
        /** Webhook 时间戳容差秒数。 */
        private long webhookToleranceSeconds = 300;
    }

    /**
     * PayPal 配置。
     */
    @Data
    public static class Paypal {
        /** 是否启用。 */
        private boolean enabled;
        /** PayPal Client ID。 */
        private String clientId;
        /** PayPal Client Secret。 */
        private String clientSecret;
        /** PayPal Webhook ID。 */
        private String webhookId;
        /** PayPal API 根地址。 */
        private String apiBaseUrl = "https://api-m.paypal.com";
        /** 支付完成返回地址。 */
        private String returnUrl;
        /** 取消支付返回地址。 */
        private String cancelUrl;
    }
}
