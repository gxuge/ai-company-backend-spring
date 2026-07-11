package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 手动文本导入请求。
 */
@Data
@Schema(description = "手动文本导入请求")
public class ImportTextDTO {
    /**
     * 文档名称。
     */
    @NotBlank(message = "文档名称不能为空")
    @JsonProperty("document_name")
    @Schema(description = "文档名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String documentName;

    /**
     * 文本内容。
     */
    @NotBlank(message = "内容不能为空")
    @JsonProperty("content")
    @Schema(description = "文本内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    /**
     * 分段长度。
     */
    @Min(value = 1, message = "chunk_size必须大于0")
    @JsonProperty("chunk_size")
    @Schema(description = "分段长度")
    private Integer chunkSize;

    /**
     * 分段重叠长度。
     */
    @Min(value = 0, message = "chunk_overlap不能小于0")
    @JsonProperty("chunk_overlap")
    @Schema(description = "分段重叠长度")
    private Integer chunkOverlap;
}
