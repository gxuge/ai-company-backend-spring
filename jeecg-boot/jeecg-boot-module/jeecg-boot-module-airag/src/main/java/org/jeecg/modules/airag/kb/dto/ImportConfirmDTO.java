package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 基于预览结果确认导入请求。
 */
@Data
@Schema(description = "基于预览结果确认导入请求")
public class ImportConfirmDTO {
    /**
     * 文档名称。
     */
    @NotBlank(message = "文档名称不能为空")
    @JsonProperty("document_name")
    @Schema(description = "文档名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String documentName;

    /**
     * 来源类型。
     */
    @NotBlank(message = "来源类型不能为空")
    @Pattern(regexp = "^(manual|file)$", message = "来源类型只能是manual或file")
    @JsonProperty("source_type")
    @Schema(description = "来源类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceType;

    /**
     * 文件类型。
     */
    @NotBlank(message = "文件类型不能为空")
    @JsonProperty("file_type")
    @Schema(description = "文件类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileType;

    /**
     * 分段列表。
     */
    @NotEmpty(message = "chunks不能为空")
    @Valid
    @JsonProperty("chunks")
    @Schema(description = "分段列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ChunkItem> chunks;

    /**
     * 预览分段项。
     */
    @Data
    @Schema(description = "预览分段项")
    public static class ChunkItem {
        /**
         * 分段内容。
         */
        @NotBlank(message = "分段内容不能为空")
        @JsonProperty("content")
        @Schema(description = "分段内容", requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;

        /**
         * 排序号。
         */
        @Min(value = 1, message = "sort_no必须大于0")
        @JsonProperty("sort_no")
        @Schema(description = "排序号", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer sortNo;

        /**
         * token估算值。
         */
        @Min(value = 0, message = "token_count不能小于0")
        @JsonProperty("token_count")
        @Schema(description = "token估算值")
        private Integer tokenCount;
    }
}
