package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * QA批量新增请求。
 */
@Data
@Schema(description = "QA批量新增请求")
public class KbQaBatchDTO {
    /**
     * QA条目列表。
     */
    @NotEmpty(message = "items不能为空")
    @JsonProperty("items")
    @Schema(description = "QA条目列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<KbQaItemDTO> items;
}
