package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * chunk列表查询请求。
 */
@Data
@Schema(description = "chunk列表查询请求")
public class KbChunkQueryDto {
    /**
     * 文档ID。
     */
    @Schema(description = "文档ID")
    private String documentId;

    /**
     * 分段类型。
     */
    @Schema(description = "分段类型")
    @Pattern(regexp = "^(text|table|code|qa)$", message = "分段类型只能是text、table、code、qa")
    private String chunkType;

    /**
     * 状态：1启用 0禁用。
     */
    @Schema(description = "状态：1启用 0禁用")
    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;

    /**
     * 页码。
     */
    @Schema(description = "页码")
    private Integer pageNo = 1;

    /**
     * 每页条数。
     */
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
