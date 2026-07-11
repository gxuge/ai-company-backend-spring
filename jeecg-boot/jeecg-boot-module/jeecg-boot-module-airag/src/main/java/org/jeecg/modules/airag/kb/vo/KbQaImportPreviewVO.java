package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * QA导入预览结果。
 */
@Data
@Schema(description = "QA导入预览结果")
public class KbQaImportPreviewVO {
    /**
     * 总数。
     */
    @JsonProperty("total")
    @Schema(description = "总数")
    private Integer total;

    /**
     * 有效数。
     */
    @JsonProperty("success_count")
    @Schema(description = "有效数")
    private Integer successCount;

    /**
     * 无效数。
     */
    @JsonProperty("failed_count")
    @Schema(description = "无效数")
    private Integer failedCount;

    /**
     * 预览行。
     */
    @JsonProperty("items")
    @Schema(description = "预览行")
    private List<KbQaImportRowVO> items = new ArrayList<>();
}
