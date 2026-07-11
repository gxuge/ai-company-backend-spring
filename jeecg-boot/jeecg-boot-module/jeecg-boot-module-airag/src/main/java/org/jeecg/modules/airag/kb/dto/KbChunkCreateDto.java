package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.entity.KbChunk;

import java.util.List;

/**
 * 创建chunk请求。
 */
@Data
@Schema(description = "创建chunk请求")
public class KbChunkCreateDto {
    /**
     * 文档ID。
     */
    @NotBlank(message = "文档ID不能为空")
    @Schema(description = "文档ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String documentId;

    /**
     * 分段内容。
     */
    @NotBlank(message = "分段内容不能为空")
    @Schema(description = "分段内容", requiredMode = Schema.RequiredMode.REQUIRED)
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
     * 转换为实体。
     *
     * @param kbId 知识库ID
     * @return chunk实体
     */
    public KbChunk toEntity(String kbId) {
        KbChunk entity = new KbChunk();
        entity.setKbId(kbId);
        entity.setDocumentId(documentId);
        entity.setContent(content);
        entity.setChunkType(oConvertUtils.isEmpty(chunkType) ? KbConstants.CHUNK_TYPE_TEXT : chunkType);
        entity.setTokenCount(tokenCount == null ? 0 : tokenCount);
        entity.setSortNo(sortNo == null ? 0 : sortNo);
        entity.setStatus(status == null ? KbConstants.STATUS_ENABLE : status);
        entity.setMetadataJson(metadataJson);
        return entity;
    }
}
