package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.entity.KbBase;

/**
 * 创建知识库请求。
 */
@Data
@Schema(description = "创建知识库请求")
public class KbBaseCreateDto {
    /**
     * 知识库名称。
     */
    @NotBlank(message = "知识库名称不能为空")
    @Schema(description = "知识库名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 知识库描述。
     */
    @Schema(description = "知识库描述")
    private String description;

    /**
     * 业务类型。
     */
    @Schema(description = "业务类型")
    private String bizType;

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
     * @return 知识库实体
     */
    public KbBase toEntity() {
        KbBase entity = new KbBase();
        entity.setName(name);
        entity.setDescription(description);
        entity.setBizType(oConvertUtils.isEmpty(bizType) ? KbConstants.DEFAULT_BIZ_TYPE : bizType);
        entity.setStatus(status == null ? KbConstants.STATUS_ENABLE : status);
        return entity;
    }
}
