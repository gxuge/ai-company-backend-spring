package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.entity.KbDocument;

/**
 * 创建文档请求。
 */
@Data
@Schema(description = "创建文档请求")
public class KbDocumentCreateDto {
    /**
     * 文档名称。
     */
    @NotBlank(message = "文档名称不能为空")
    @Schema(description = "文档名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 来源类型：manual/upload/url/import。
     */
    @Schema(description = "来源类型：manual/upload/url/import")
    @Pattern(regexp = "^(manual|upload|url|import|qa)$", message = "来源类型只能是manual、upload、url、import、qa")
    private String sourceType;

    /**
     * 文件类型。
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 文件地址。
     */
    @Schema(description = "文件地址")
    private String fileUrl;

    /**
     * 解析状态：pending/processing/success/failed。
     */
    @Schema(description = "解析状态：pending/processing/success/failed")
    @Pattern(regexp = "^(pending|processing|success|failed)$", message = "解析状态只能是pending、processing、success、failed")
    private String parseStatus;

    /**
     * 切分状态：pending/processing/success/failed。
     */
    @Schema(description = "切分状态：pending/processing/success/failed")
    @Pattern(regexp = "^(pending|processing|success|failed)$", message = "切分状态只能是pending、processing、success、failed")
    private String chunkStatus;

    /**
     * 向量状态：pending/processing/success/failed。
     */
    @Schema(description = "向量状态：pending/processing/success/failed")
    @Pattern(regexp = "^(pending|processing|success|failed)$", message = "向量状态只能是pending、processing、success、failed")
    private String embedStatus;

    /**
     * 元数据JSON。
     */
    @Schema(description = "元数据JSON")
    private String metadataJson;

    /**
     * 状态：1启用 0禁用。
     */
    @Schema(description = "状态：1启用 0禁用")
    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;

    /**
     * 转换为实体。
     *
     * @param kbId 知识库ID
     * @return 文档实体
     */
    public KbDocument toEntity(String kbId) {
        KbDocument entity = new KbDocument();
        entity.setKbId(kbId);
        entity.setName(name);
        entity.setSourceType(oConvertUtils.isEmpty(sourceType) ? KbConstants.SOURCE_TYPE_MANUAL : sourceType);
        entity.setFileType(fileType);
        entity.setFileUrl(fileUrl);
        entity.setParseStatus(oConvertUtils.isEmpty(parseStatus) ? KbConstants.PROCESS_STATUS_PENDING : parseStatus);
        entity.setChunkStatus(oConvertUtils.isEmpty(chunkStatus) ? KbConstants.PROCESS_STATUS_PENDING : chunkStatus);
        entity.setEmbedStatus(oConvertUtils.isEmpty(embedStatus) ? KbConstants.PROCESS_STATUS_PENDING : embedStatus);
        entity.setMetadataJson(metadataJson);
        entity.setStatus(status == null ? KbConstants.STATUS_ENABLE : status);
        return entity;
    }
}
