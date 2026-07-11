package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;

/**
 * chunk索引文本保存请求。
 */
@Data
@Schema(description = "chunk索引文本保存请求")
public class KbChunkIndexSaveDto {
    /**
     * 索引文本。
     */
    @Schema(description = "索引文本")
    @NotBlank(message = "索引文本不能为空")
    @Size(max = 4000, message = "索引文本不能超过4000个字符")
    private String indexText;

    /**
     * 索引类型：default/title/question/summary等。
     */
    @Schema(description = "索引类型：default/title/question/summary等")
    @Pattern(regexp = "^(default|title|question|summary|manual|keyword|auto_question)$", message = "索引类型只能是default、title、question、summary、manual、keyword、auto_question")
    private String indexType;

    /**
     * 向量状态：pending/processing/success/failed。
     */
    @Schema(description = "向量状态：pending/processing/success/failed")
    @Pattern(regexp = "^(pending|processing|success|failed)$", message = "向量状态只能是pending、processing、success、failed")
    private String embeddingStatus;

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
     * 转换为实体。
     *
     * @param kbId 知识库ID
     * @param chunkId 分段ID
     * @return chunk索引实体
     */
    public KbChunkIndex toEntity(String kbId, String chunkId) {
        KbChunkIndex entity = new KbChunkIndex();
        entity.setKbId(kbId);
        entity.setChunkId(chunkId);
        entity.setIndexText(indexText);
        entity.setIndexType(oConvertUtils.isEmpty(indexType) ? KbConstants.INDEX_TYPE_DEFAULT : indexType);
        entity.setEmbeddingStatus(oConvertUtils.isEmpty(embeddingStatus) ? KbConstants.PROCESS_STATUS_PENDING : embeddingStatus);
        entity.setSortNo(sortNo == null ? 0 : sortNo);
        entity.setStatus(status == null ? KbConstants.STATUS_ENABLE : status);
        entity.setMetadataJson(metadataJson);
        return entity;
    }
}
