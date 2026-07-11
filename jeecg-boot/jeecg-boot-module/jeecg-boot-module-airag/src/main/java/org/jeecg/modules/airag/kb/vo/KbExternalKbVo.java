package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.airag.kb.entity.KbExternalKb;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 外部知识库返回对象。
 */
@Data
@Schema(description = "外部知识库返回对象")
public class KbExternalKbVo {
    /**
     * 主键ID。
     */
    @Schema(description = "主键ID")
    private String id;

    /**
     * 外部知识库ID。
     */
    @Schema(description = "外部知识库ID")
    @JsonProperty("external_kb_id")
    private String externalKbId;

    /**
     * 名称。
     */
    @Schema(description = "名称")
    private String name;

    /**
     * 是否启用。
     */
    @Schema(description = "是否启用")
    private Boolean enabled;

    /**
     * 接口地址。
     */
    @Schema(description = "接口地址")
    @JsonProperty("endpoint_url")
    private String endpointUrl;

    /**
     * 鉴权类型。
     */
    @Schema(description = "鉴权类型")
    @JsonProperty("auth_type")
    private String authType;

    /**
     * 鉴权配置。
     */
    @Schema(description = "鉴权配置")
    @JsonProperty("auth_config")
    private String authConfig;

    /**
     * 超时时间。
     */
    @Schema(description = "超时时间")
    @JsonProperty("timeout_ms")
    private Integer timeoutMs;

    /**
     * 权重。
     */
    @Schema(description = "权重")
    private BigDecimal weight;

    /**
     * 元数据JSON。
     */
    @Schema(description = "元数据JSON")
    @JsonProperty("metadata_json")
    private String metadataJson;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("created_at")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("updated_at")
    private Date updatedAt;

    /**
     * 从实体转换。
     *
     * @param entity 实体
     * @return 返回对象
     */
    public static KbExternalKbVo from(KbExternalKb entity) {
        if (entity == null) {
            return null;
        }
        KbExternalKbVo vo = new KbExternalKbVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
