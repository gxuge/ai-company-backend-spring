package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 知识库列表查询请求。
 */
@Data
@Schema(description = "知识库列表查询请求")
public class KbBaseQueryDto {
    /**
     * 知识库名称。
     */
    @Schema(description = "知识库名称")
    private String name;

    /**
     * 业务类型。
     */
    @Schema(description = "业务类型")
    private String bizType;

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
