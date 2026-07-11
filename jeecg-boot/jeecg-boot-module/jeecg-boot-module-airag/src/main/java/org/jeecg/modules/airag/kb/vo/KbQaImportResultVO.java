package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * QA导入结果。
 */
@Data
@Schema(description = "QA导入结果")
public class KbQaImportResultVO {
    /**
     * 总数。
     */
    @JsonProperty("total")
    @Schema(description = "总数")
    private Integer total;

    /**
     * 成功数。
     */
    @JsonProperty("success_count")
    @Schema(description = "成功数")
    private Integer successCount;

    /**
     * 失败数。
     */
    @JsonProperty("failed_count")
    @Schema(description = "失败数")
    private Integer failedCount;

    /**
     * 跳过数。
     */
    @JsonProperty("skipped_count")
    @Schema(description = "跳过数")
    private Integer skippedCount;

    /**
     * 错误列表。
     */
    @JsonProperty("errors")
    @Schema(description = "错误列表")
    private List<KbQaImportRowVO> errors = new ArrayList<>();
}
