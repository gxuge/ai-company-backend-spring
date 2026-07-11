package org.jeecg.modules.airag.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 外部知识库配置。
 */
@Data
@Schema(description = "外部知识库配置")
@TableName("kb_external_kb")
public class KbExternalKb implements Serializable {
    /**
     * 主键ID。
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /**
     * 外部知识库ID。
     */
    @Schema(description = "外部知识库ID")
    @TableField("external_kb_id")
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
    @TableField("endpoint_url")
    private String endpointUrl;

    /**
     * 鉴权类型。
     */
    @Schema(description = "鉴权类型")
    @TableField("auth_type")
    private String authType;

    /**
     * 鉴权配置。
     */
    @Schema(description = "鉴权配置")
    @TableField("auth_config")
    private String authConfig;

    /**
     * 超时时间。
     */
    @Schema(description = "超时时间")
    @TableField("timeout_ms")
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
    @TableField("metadata_json")
    private String metadataJson;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("created_at")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("updated_at")
    private Date updatedAt;
}
