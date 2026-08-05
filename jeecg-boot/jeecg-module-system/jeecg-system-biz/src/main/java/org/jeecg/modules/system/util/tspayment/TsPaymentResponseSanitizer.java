package org.jeecg.modules.system.util.tspayment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 支付渠道响应脱敏工具。 */
public final class TsPaymentResponseSanitizer {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "client_secret",
            "clientsecret",
            "access_token",
            "accesstoken",
            "email",
            "email_address",
            "phone",
            "phone_number",
            "account_number",
            "card",
            "billing_details",
            "shipping",
            "payer");

    private TsPaymentResponseSanitizer() {
    }

    /** 脱敏 JSON 响应；非 JSON 内容不向管理端透传。 */
    public static String sanitize(String rawResponse, ObjectMapper objectMapper) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return rawResponse;
        }
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            sanitizeNode(root);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception ignored) {
            return "非JSON响应内容已隐藏";
        }
    }

    /** 递归屏蔽对象及数组中的敏感字段。 */
    private static void sanitizeNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey().toLowerCase(Locale.ROOT);
                if (SENSITIVE_FIELDS.contains(fieldName)) {
                    objectNode.put(field.getKey(), "***");
                } else {
                    sanitizeNode(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(TsPaymentResponseSanitizer::sanitizeNode);
        }
    }
}
