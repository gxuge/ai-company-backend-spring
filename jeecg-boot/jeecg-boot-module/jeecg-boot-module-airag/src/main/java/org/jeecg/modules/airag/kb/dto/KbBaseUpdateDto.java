package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.entity.KbBase;

/**
 * 更新知识库请求。
 */
@Data
@Schema(description = "更新知识库请求")
public class KbBaseUpdateDto {
    /**
     * 知识库名称。
     */
    @Schema(description = "知识库名称")
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
     * 应用到实体。
     *
     * @param entity 知识库实体
     */
    public void applyTo(KbBase entity) {
        if (entity == null) {
            return;
        }
        if (name != null) {
            entity.setName(name);
        }
        if (description != null) {
            entity.setDescription(description);
        }
        if (bizType != null) {
            entity.setBizType(bizType);
        } else if (oConvertUtils.isEmpty(entity.getBizType())) {
            entity.setBizType(KbConstants.DEFAULT_BIZ_TYPE);
        }
        if (status != null) {
            entity.setStatus(status);
        }
    }
}
