package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 文档列表查询请求。
 */
@Data
@Schema(description = "文档列表查询请求")
public class KbDocumentQueryDto {
    /**
     * 文档名称。
     */
    @Schema(description = "文档名称")
    private String name;

    /**
     * 来源类型。
     */
    @Schema(description = "来源类型")
    @Pattern(regexp = "^(manual|upload|url|import)$", message = "来源类型只能是manual、upload、url、import")
    private String sourceType;

    /**
     * 解析状态。
     */
    @Schema(description = "解析状态")
    @Pattern(regexp = "^(pending|processing|success|failed)$", message = "解析状态只能是pending、processing、success、failed")
    private String parseStatus;

    /**
     * 切分状态。
     */
    @Schema(description = "切分状态")
    @Pattern(regexp = "^(pending|processing|success|failed)$", message = "切分状态只能是pending、processing、success、failed")
    private String chunkStatus;

    /**
     * 向量状态。
     */
    @Schema(description = "向量状态")
    @Pattern(regexp = "^(pending|processing|success|failed)$", message = "向量状态只能是pending、processing、success、failed")
    private String embedStatus;

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
