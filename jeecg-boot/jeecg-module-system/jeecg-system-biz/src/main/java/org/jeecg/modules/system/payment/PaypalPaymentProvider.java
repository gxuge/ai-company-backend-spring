package org.jeecg.modules.system.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PayPal Checkout Orders 支付渠道。
 */
@Component
public class PaypalPaymentProvider implements PaymentProvider {

    private static final String PROVIDER = "PAYPAL";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final TsPaymentConfigBean.Paypal config;

    /**
     * 创建 PayPal HTTP 客户端。
     */
    public PaypalPaymentProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            TsPaymentConfigBean paymentConfig) {
        this.objectMapper = objectMapper;
        this.config = paymentConfig.getPaypal();
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
        Map<String, Object> purchaseUnit = new LinkedHashMap<>();
        purchaseUnit.put("custom_id", command.getOrderNo());
        purchaseUnit.put("invoice_id", command.getOrderNo());
        purchaseUnit.put("description", command.getDescription());
        purchaseUnit.put("amount", Map.of(
                "currency_code", command.getCurrency(),
                "value", command.getAmount().setScale(2).toPlainString()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("intent", "CAPTURE");
        body.put("purchase_units", List.of(purchaseUnit));
        Map<String, Object> applicationContext = new LinkedHashMap<>();
        applicationContext.put("user_action", "PAY_NOW");
        if (StringUtils.hasText(config.getReturnUrl())) {
            applicationContext.put("return_url", config.getReturnUrl().trim());
        }
        if (StringUtils.hasText(config.getCancelUrl())) {
            applicationContext.put("cancel_url", config.getCancelUrl().trim());
        }
        body.put("application_context", applicationContext);

        String response = execute(() -> restClient.post()
                .uri("/v2/checkout/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .header("PayPal-Request-Id", command.getOrderNo())
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class));
        JsonNode root = readJson(response);
        return PaymentProviderResult.builder()
                .paymentIntentId(requiredText(root, "id"))
                .transactionId(captureId(root))
                .status(mapStatus(text(root, "status")))
                .paymentUrl(findLink(root, "approve"))
                .rawResponse(response)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public PaymentProviderResult queryPayment(PaymentQueryCommand command) {
        requireEnabled();
        String accessToken = accessToken();
        String response = execute(() -> restClient.get()
                .uri("/v2/checkout/orders/{id}", command.getPaymentIntentId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(String.class));
        JsonNode root = readJson(response);
        if ("APPROVED".equalsIgnoreCase(text(root, "status"))) {
            response = execute(() -> restClient.post()
                    .uri("/v2/checkout/orders/{id}/capture", command.getPaymentIntentId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("PayPal-Request-Id", command.getPaymentIntentId() + "-capture")
                    .header("Prefer", "return=representation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(String.class));
            root = readJson(response);
        }
        return PaymentProviderResult.builder()
                .paymentIntentId(requiredText(root, "id"))
                .transactionId(captureId(root))
                .status(mapStatus(text(root, "status")))
                .paymentUrl(findLink(root, "approve"))
                .rawResponse(response)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public PaymentCallbackResult handleCallback(PaymentCallbackCommand command) {
        requireEnabled();
        verifyWebhook(command);
        JsonNode root = readJson(command.getRawBody());
        String eventType = requiredText(root, "event_type");
        String status;
        if ("PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
            status = "SUCCEEDED";
        } else if ("PAYMENT.CAPTURE.DENIED".equals(eventType)
                || "PAYMENT.CAPTURE.DECLINED".equals(eventType)) {
            status = "FAILED";
        } else if ("PAYMENT.CAPTURE.REFUNDED".equals(eventType)
                || "PAYMENT.CAPTURE.REVERSED".equals(eventType)) {
            status = "CANCELED";
        } else {
            return PaymentCallbackResult.builder()
                    .processable(false)
                    .rawResponse(command.getRawBody())
                    .build();
        }
        JsonNode resource = root.path("resource");
        String paymentIntentId = resource.path("supplementary_data")
                .path("related_ids").path("order_id").asText(null);
        if (!StringUtils.hasText(paymentIntentId)) {
            throw new JeecgBootBizTipException("PayPal回调缺少支付订单ID");
        }
        return PaymentCallbackResult.builder()
                .processable(true)
                .paymentIntentId(paymentIntentId)
                .transactionId(text(resource, "id"))
                .status(status)
                .amount(new BigDecimal(requiredText(resource.path("amount"), "value")))
                .currency(requiredText(resource.path("amount"), "currency_code"))
                .rawResponse(command.getRawBody())
                .build();
    }

    /**
     * 调用 PayPal 验签接口校验原始 webhook。
     */
    private void verifyWebhook(PaymentCallbackCommand command) {
        if (!StringUtils.hasText(config.getWebhookId())) {
            throw new JeecgBootBizTipException("PayPal webhook ID未配置");
        }
        Map<String, String> headers = command.getHeaders();
        List<String> requiredHeaders = List.of(
                "paypal-transmission-id",
                "paypal-transmission-time",
                "paypal-cert-url",
                "paypal-auth-algo",
                "paypal-transmission-sig");
        for (String header : requiredHeaders) {
            if (!StringUtils.hasText(headers.get(header))) {
                throw new JeecgBootBizTipException("PayPal webhook请求头缺失：" + header);
            }
        }
        String verificationBody;
        try {
            verificationBody = "{"
                    + "\"transmission_id\":" + objectMapper.writeValueAsString(
                    headers.get("paypal-transmission-id")) + ","
                    + "\"transmission_time\":" + objectMapper.writeValueAsString(
                    headers.get("paypal-transmission-time")) + ","
                    + "\"cert_url\":" + objectMapper.writeValueAsString(
                    headers.get("paypal-cert-url")) + ","
                    + "\"auth_algo\":" + objectMapper.writeValueAsString(
                    headers.get("paypal-auth-algo")) + ","
                    + "\"transmission_sig\":" + objectMapper.writeValueAsString(
                    headers.get("paypal-transmission-sig")) + ","
                    + "\"webhook_id\":" + objectMapper.writeValueAsString(
                    config.getWebhookId().trim()) + ","
                    + "\"webhook_event\":" + command.getRawBody()
                    + "}";
        } catch (Exception ex) {
            throw new JeecgBootBizTipException("PayPal webhook验签参数构建失败");
        }
        String response = execute(() -> restClient.post()
                .uri("/v1/notifications/verify-webhook-signature")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(verificationBody)
                .retrieve()
                .body(String.class));
        if (!"SUCCESS".equalsIgnoreCase(text(readJson(response), "verification_status"))) {
            throw new JeecgBootBizTipException("PayPal webhook签名验证失败");
        }
    }

    /**
     * 获取 PayPal OAuth2 访问令牌。
     */
    private String accessToken() {
        String credentials = config.getClientId().trim() + ":" + config.getClientSecret().trim();
        String basic = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        String response = execute(() -> restClient.post()
                .uri("/v1/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class));
        return requiredText(readJson(response), "access_token");
    }

    /**
     * 读取 PayPal 审批链接。
     */
    private String findLink(JsonNode root, String relation) {
        for (JsonNode link : root.path("links")) {
            if (relation.equals(link.path("rel").asText())) {
                return link.path("href").asText(null);
            }
        }
        return null;
    }

    /**
     * 读取订单首个 capture ID。
     */
    private String captureId(JsonNode root) {
        JsonNode purchaseUnits = root.path("purchase_units");
        if (!purchaseUnits.isArray() || purchaseUnits.isEmpty()) {
            return null;
        }
        JsonNode captures = purchaseUnits.get(0).path("payments").path("captures");
        return captures.isArray() && !captures.isEmpty()
                ? captures.get(0).path("id").asText(null)
                : null;
    }

    /**
     * 统一 PayPal 支付状态。
     */
    private String mapStatus(String status) {
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "SUCCEEDED";
        }
        if ("VOIDED".equalsIgnoreCase(status)) {
            return "CANCELED";
        }
        return "PENDING";
    }

    /**
     * 校验 PayPal 配置已启用。
     */
    private void requireEnabled() {
        if (!config.isEnabled()
                || !StringUtils.hasText(config.getClientId())
                || !StringUtils.hasText(config.getClientSecret())) {
            throw new JeecgBootBizTipException("PayPal支付渠道未配置");
        }
    }

    /**
     * 解析 JSON。
     */
    private JsonNode readJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception ex) {
            throw new JeecgBootBizTipException("PayPal响应格式不正确");
        }
    }

    /**
     * 读取必填文本字段。
     */
    private String requiredText(JsonNode node, String field) {
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            throw new JeecgBootBizTipException("PayPal响应缺少字段：" + field);
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
     * 统一转换 PayPal HTTP 异常。
     */
    private String execute(HttpCall call) {
        try {
            String response = call.execute();
            if (!StringUtils.hasText(response)) {
                throw new JeecgBootBizTipException("PayPal响应为空");
            }
            return response;
        } catch (RestClientResponseException ex) {
            throw new JeecgBootBizTipException(
                    "PayPal支付请求失败：" + ex.getStatusCode().value());
        }
    }

    /**
     * PayPal HTTP 调用。
     */
    @FunctionalInterface
    private interface HttpCall {
        String execute();
    }
}
