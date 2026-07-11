package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 知识库主表返回对象。
 */
@Data
@Schema(description = "知识库主表返回对象")
public class KbBaseVo {
    /**
     * 主键ID。
     */
    @Schema(description = "主键ID")
    private String id;

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
    private Integer status;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /**
     * 搜索配置。
     */
    @Schema(description = "搜索配置")
    private KbSearchConfigVo searchConfig;

    /**
     * 由实体转换为返回对象。
     *
     * @param entity 知识库实体
     * @return 返回对象
     */
    public static KbBaseVo from(KbBase entity) {
        if (entity == null) {
            return null;
        }
        KbBaseVo vo = new KbBaseVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
