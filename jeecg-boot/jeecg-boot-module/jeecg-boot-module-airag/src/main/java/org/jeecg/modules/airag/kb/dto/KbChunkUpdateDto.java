package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.entity.KbChunk;

import java.util.List;

/**
 * 更新chunk请求。
 */
@Data
@Schema(description = "更新chunk请求")
public class KbChunkUpdateDto {
    /**
     * 分段内容。
     */
    @Schema(description = "分段内容")
    @NotBlank(message = "分段内容不能为空")
    private String content;

    /**
     * 分段类型：text/table/code/qa等。
     */
    @Schema(description = "分段类型：text/table/code/qa等")
    @Pattern(regexp = "^(text|table|code|qa)$", message = "分段类型只能是text、table、code、qa")
    private String chunkType;

    /**
     * Token数量。
     */
    @Schema(description = "Token数量")
    private Integer tokenCount;

    /**
     * 排序号。
     */
    @Schema(description = "排序号")
    private Integer sortNo;

    /**
     * 状态：1启用 0禁用。
     */
    @Schema(description = "状态：1启用 0禁用")
    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;

    /**
     * 元数据JSON。
     */
    @Schema(description = "元数据JSON")
    private String metadataJson;

    /**
     * chunk索引文本列表。
     */
    @Valid
    @Schema(description = "chunk索引文本列表")
    private List<KbChunkIndexSaveDto> indexList;

    /**
     * 应用到实体。
     *
     * @param entity chunk实体
     */
    public void applyTo(KbChunk entity) {
        if (entity == null) {
            return;
        }
        if (content != null) {
            entity.setContent(content);
        }
        if (chunkType != null) {
            entity.setChunkType(chunkType);
        } else if (oConvertUtils.isEmpty(entity.getChunkType())) {
            entity.setChunkType(KbConstants.CHUNK_TYPE_TEXT);
        }
        if (tokenCount != null) {
            entity.setTokenCount(tokenCount);
        }
        if (sortNo != null) {
            entity.setSortNo(sortNo);
        }
        if (status != null) {
            entity.setStatus(status);
        }
        if (metadataJson != null) {
            entity.setMetadataJson(metadataJson);
        }
    }
}
