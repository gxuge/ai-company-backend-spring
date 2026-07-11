package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * QA确认导入请求。
 */
@Data
@Schema(description = "QA确认导入请求")
public class KbQaImportConfirmDTO {
    /**
     * 文档名称。
     */
    @NotBlank(message = "document_name不能为空")
    @JsonProperty("document_name")
    @Schema(description = "文档名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String documentName;

    /**
     * 来源类型。
     */
    @JsonProperty("source_type")
    @Schema(description = "来源类型")
    private String sourceType;

    /**
     * 文件类型。
     */
    @JsonProperty("file_type")
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * QA条目列表。
     */
    @NotEmpty(message = "items不能为空")
    @JsonProperty("items")
    @Schema(description = "QA条目列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<KbQaItemDTO> items;
}
