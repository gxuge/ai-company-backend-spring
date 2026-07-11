package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.jeecg.modules.airag.kb.entity.KbExternalKb;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 外部知识库保存请求。
 */
@Data
@Schema(description = "外部知识库保存请求")
public class KbExternalKbSaveDto {
    /**
     * 外部知识库ID。
     */
    @NotBlank(message = "external_kb_id不能为空")
    @JsonProperty("external_kb_id")
    @Schema(description = "外部知识库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String externalKbId;

    /**
     * 名称。
     */
    @NotBlank(message = "name不能为空")
    @JsonProperty("name")
    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 是否启用。
     */
    @NotNull(message = "enabled不能为空")
    @JsonProperty("enabled")
    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;

    /**
     * 接口地址。
     */
    @NotBlank(message = "endpoint_url不能为空")
    @JsonProperty("endpoint_url")
    @Schema(description = "接口地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String endpointUrl;

    /**
     * 鉴权类型。
     */
    @Pattern(regexp = "^(none|api_key|bearer)$", message = "auth_type只能是none、api_key、bearer")
    @JsonProperty("auth_type")
    @Schema(description = "鉴权类型")
    private String authType;

    /**
     * 鉴权配置。
     */
    @JsonProperty("auth_config")
    @Schema(description = "鉴权配置")
    private String authConfig;

    /**
     * 超时时间。
     */
    @Min(value = 1, message = "timeout_ms必须大于0")
    @JsonProperty("timeout_ms")
    @Schema(description = "超时时间")
    private Integer timeoutMs;

    /**
     * 权重。
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "weight不能小于0")
    @JsonProperty("weight")
    @Schema(description = "权重")
    private BigDecimal weight;

    /**
     * 元数据JSON。
     */
    @JsonProperty("metadata_json")
    @Schema(description = "元数据JSON")
    private String metadataJson;

    /**
     * 转换为实体。
     *
     * @return 实体
     */
    public KbExternalKb toEntity() {
        KbExternalKb entity = new KbExternalKb();
        Date now = new Date();
        entity.setExternalKbId(externalKbId);
        entity.setName(name);
        entity.setEnabled(enabled);
        entity.setEndpointUrl(endpointUrl);
        entity.setAuthType(authType == null ? "none" : authType);
        entity.setAuthConfig(authConfig);
        entity.setTimeoutMs(timeoutMs == null ? 5000 : timeoutMs);
        entity.setWeight(weight == null ? BigDecimal.ONE : weight);
        entity.setMetadataJson(metadataJson);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
