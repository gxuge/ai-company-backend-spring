package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 外部知识库查询请求。
 */
@Data
@Schema(description = "外部知识库查询请求")
public class KbExternalKbQueryDto {
    /**
     * 外部知识库ID。
     */
    @JsonProperty("external_kb_id")
    @Schema(description = "外部知识库ID")
    private String externalKbId;

    /**
     * 名称关键字。
     */
    @JsonProperty("name")
    @Schema(description = "名称关键字")
    private String name;

    /**
     * 是否启用。
     */
    @JsonProperty("enabled")
    @Schema(description = "是否启用")
    private Boolean enabled;

    /**
     * 页码。
     */
    @Min(value = 1, message = "page_no必须大于0")
    @JsonProperty("page_no")
    @Schema(description = "页码")
    private Integer pageNo;

    /**
     * 每页大小。
     */
    @Min(value = 1, message = "page_size必须大于0")
    @Max(value = 100, message = "page_size不能超过100")
    @JsonProperty("page_size")
    @Schema(description = "每页大小")
    private Integer pageSize;
}
