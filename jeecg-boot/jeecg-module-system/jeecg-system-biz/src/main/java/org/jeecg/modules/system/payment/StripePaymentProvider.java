package org.jeecg.modules.system.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.system.config.TsPaymentConfigBean;
import org.jeecg.modules.system.payment.model.PaymentCallbackCommand;
import org.jeecg.modules.system.payment.model.PaymentCallbackResult;
import org.jeecg.modules.system.payment.model.PaymentCreateCommand;
import org.jeecg.modules.system.payment.model.PaymentProviderResult;
import org.jeecg.modules.system.payment.model.PaymentQueryCommand;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Stripe PaymentIntent 支付渠道。
 */
@Component
public class StripePaymentProvider implements PaymentProvider {

    private static final String PROVIDER = "STRIPE";
    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA",
            "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final TsPaymentConfigBean.Stripe config;

    /**
     * 创建 Stripe HTTP 客户端。
     */
    public StripePaymentProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            TsPaymentConfigBean paymentConfig) {
        this.objectMapper = objectMapper;
        this.config = paymentConfig.getStripe();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(paymentConfig.getConnectTimeoutMs(), 1000));
        requestFactory.setReadTimeout(Math.max(paymentConfig.getReadTimeoutMs(), 1000));
        this.restClient = builder.clone()
                .requestFactory(requestFactory)
                .baseUrl(config.getApiBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public String providerCode() {
        return PROVIDER;
    }

    /** {@inheritDoc} */
    @Override
    public PaymentProviderResult createPayment(PaymentCreateCommand command) {
        requireEnabled();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("amount", toMinorAmount(command.getAmount(), command.getCurrency()).toPlainString());
        form.add("currency", command.getCurrency().toLowerCase(Locale.ROOT));
        form.add("automatic_payment_methods[enabled]", "true");
        form.add("description", command.getDescription());
        form.add("metadata[orderNo]", command.getOrderNo());
        String response = execute(() -> restClient.post()
                .uri("/v1/payment_intents")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getSecretKey().trim())
                .header("Idempotency-Key", command.getOrderNo())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class));
        JsonNode root = readJson(response);
        return PaymentProviderResult.builder()
                .paymentIntentId(requiredText(root, "id"))
                .transactionId(text(root, "latest_charge"))
                .status(mapStatus(text(root, "status")))
                .clientSecret(text(root, "client_secret"))
                .rawResponse(sanitize(response))
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public PaymentProviderResult queryPayment(PaymentQueryCommand command) {
        requireEnabled();
        String response = execute(() -> restClient.get()
                .uri("/v1/payment_intents/{id}", command.getPaymentIntentId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getSecretKey().trim())
                .retrieve()
                .body(String.class));
        JsonNode root = readJson(response);
        return PaymentProviderResult.builder()
                .paymentIntentId(requiredText(root, "id"))
                .transactionId(text(root, "latest_charge"))
                .status(mapStatus(text(root, "status")))
                .clientSecret(text(root, "client_secret"))
                .rawResponse(sanitize(response))
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public PaymentCallbackResult handleCallback(PaymentCallbackCommand command) {
        requireEnabled();
        verifySignature(command.getRawBody(), command.getHeaders().get("stripe-signature"));
        JsonNode root = readJson(command.getRawBody());
        String eventType = requiredText(root, "type");
        JsonNode paymentIntent = root.path("data").path("object");
        if (!eventType.startsWith("payment_intent.")) {
            return PaymentCallbackResult.builder()
                    .processable(false)
                    .rawResponse(command.getRawBody())
                    .build();
        }
        String status = switch (eventType) {
            case "payment_intent.succeeded" -> "SUCCEEDED";
            case "payment_intent.payment_failed" -> "FAILED";
            case "payment_intent.canceled" -> "CANCELED";
            default -> "PENDING";
        };
        return PaymentCallbackResult.builder()
                .processable(!"PENDING".equals(status))
                .paymentIntentId(requiredText(paymentIntent, "id"))
                .transactionId(text(paymentIntent, "latest_charge"))
                .status(status)
                .amount(fromMinorAmount(
                        paymentIntent.path("amount").asLong(),
                        requiredText(paymentIntent, "currency")))
                .currency(requiredText(paymentIntent, "currency").toUpperCase(Locale.ROOT))
                .rawResponse(command.getRawBody())
                .build();
    }

    /**
     * 校验 Stripe-Signature 时间戳和 HMAC-SHA256 签名。
     */
    private void verifySignature(String payload, String signatureHeader) {
        if (!StringUtils.hasText(config.getWebhookSecret())
                || !StringUtils.hasText(signatureHeader)) {
            throw new JeecgBootBizTipException("Stripe webhook签名配置或请求头缺失");
        }
        String timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if ("t".equals(pair[0])) {
                timestamp = pair[1];
            } else if ("v1".equals(pair[0])) {
                signatures.add(pair[1]);
            }
        }
        if (!StringUtils.hasText(timestamp) || signatures.isEmpty()) {
            throw new JeecgBootBizTipException("Stripe webhook签名格式不正确");
        }
        long eventTimestamp;
        try {
            eventTimestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new JeecgBootBizTipException("Stripe webhook时间戳不正确");
        }
        long tolerance = Math.max(config.getWebhookToleranceSeconds(), 0);
        if (tolerance > 0
                && Math.abs(Instant.now().getEpochSecond() - eventTimestamp) > tolerance) {
            throw new JeecgBootBizTipException("Stripe webhook请求已过期");
        }
        byte[] expected = hmacSha256(
                timestamp + "." + payload,
                config.getWebhookSecret().trim());
        for (String signature : signatures) {
            byte[] actual = decodeHex(signature);
            if (actual != null && MessageDigest.isEqual(expected, actual)) {
                return;
            }
        }
        throw new JeecgBootBizTipException("Stripe webhook签名验证失败");
    }

    /**
     * 计算 HMAC-SHA256。
     */
    private byte[] hmacSha256(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new JeecgBootBizTipException("Stripe webhook签名计算失败");
        }
    }

    /**
     * 解码十六进制签名。
     */
    private byte[] decodeHex(String value) {
        if (value == null || (value.length() & 1) == 1) {
            return null;
        }
        byte[] result = new byte[value.length() / 2];
        try {
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
            }
            return result;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 将金额转换为 Stripe 最小货币单位。
     */
    private BigDecimal toMinorAmount(BigDecimal amount, String currency) {
        int scale = ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase(Locale.ROOT)) ? 0 : 2;
        return amount.movePointRight(scale).setScale(0, RoundingMode.UNNECESSARY);
    }

    /**
     * 将 Stripe 最小货币单位转换为订单金额。
     */
    private BigDecimal fromMinorAmount(long amount, String currency) {
        int scale = ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase(Locale.ROOT)) ? 0 : 2;
        return BigDecimal.valueOf(amount, scale);
    }

    /**
     * 统一 Stripe 支付状态。
     */
    private String mapStatus(String status) {
        if ("succeeded".equals(status)) {
            return "SUCCEEDED";
        }
        if ("canceled".equals(status)) {
            return "CANCELED";
        }
        if ("requires_payment_method".equals(status)) {
            return "PENDING";
        }
        return "PENDING";
    }

    /**
     * 移除不应持久化的客户端密钥。
     */
    private String sanitize(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (root instanceof ObjectNode objectNode) {
                objectNode.remove("client_secret");
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 校验 Stripe 配置已启用。
     */
    private void requireEnabled() {
        if (!config.isEnabled() || !StringUtils.hasText(config.getSecretKey())) {
            throw new JeecgBootBizTipException("Stripe支付渠道未配置");
        }
    }

    /**
     * 解析 JSON。
     */
    private JsonNode readJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception ex) {
            throw new JeecgBootBizTipException("Stripe响应格式不正确");
        }
    }

    /**
     * 读取必填文本字段。
     */
    private String requiredText(JsonNode node, String field) {
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            throw new JeecgBootBizTipException("Stripe响应缺少字段：" + field);
        }
        return value;
    }

    /**
     * 读取可选文本字段。
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    /**
     * 统一转换 Stripe HTTP 异常。
     */
    private String execute(HttpCall call) {
        try {
            String response = call.execute();
            if (!StringUtils.hasText(response)) {
                throw new JeecgBootBizTipException("Stripe响应为空");
            }
            return response;
        } catch (RestClientResponseException ex) {
            throw new JeecgBootBizTipException(
                    "Stripe支付请求失败：" + ex.getStatusCode().value());
        }
    }

    /**
     * Stripe HTTP 调用。
     */
    @FunctionalInterface
    private interface HttpCall {
        String execute();
    }
}
