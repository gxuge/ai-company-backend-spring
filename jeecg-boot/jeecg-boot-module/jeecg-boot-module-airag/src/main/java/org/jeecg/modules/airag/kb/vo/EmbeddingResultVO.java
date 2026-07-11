package org.jeecg.modules.airag.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * embedding结果。
 */
@Data
@Schema(description = "embedding结果")
public class EmbeddingResultVO {
    /**
     * 向量。
     */
    @Schema(description = "向量")
    private List<Float> vector = new ArrayList<>();

    /**
     * 模型名称。
     */
    @Schema(description = "模型名称")
    private String modelName;

    /**
     * 向量维度。
     */
    @Schema(description = "向量维度")
    private Integer vectorDimension;

    /**
     * 调用耗时（毫秒）。
     */
    @Schema(description = "调用耗时（毫秒）")
    private Long durationMs;

    /**
     * 是否回退实现。
     */
    @Schema(description = "是否回退实现")
    private Boolean fallback;

    /**
     * 是否发生截断。
     */
    @Schema(description = "是否发生截断")
    private Boolean truncated;
}
