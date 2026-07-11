package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * chunk索引查询请求。
 */
@Data
@Schema(description = "chunk索引查询请求")
public class KbChunkIndexQueryDto {
    /**
     * 分段ID。
     */
    @Schema(description = "分段ID")
    private String chunkId;

    /**
     * 索引类型：default/title/question/summary等。
     */
    @Schema(description = "索引类型")
    @Pattern(regexp = "^(default|title|question|summary|manual|keyword|auto_question)$", message = "索引类型只能是default、title、question、summary、manual、keyword、auto_question")
    private String indexType;

    /**
     * 向量状态：pending/processing/success/failed。
     */
    @Schema(description = "向量状态")
    @Pattern(regexp = "^(pending|processing|success|failed)$", message = "向量状态只能是pending、processing、success、failed")
    private String embeddingStatus;

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
