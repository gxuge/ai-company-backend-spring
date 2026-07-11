package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.airag.kb.entity.KbDocument;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 知识库文档返回对象。
 */
@Data
@Schema(description = "知识库文档返回对象")
public class KbDocumentVo {
    /**
     * 主键ID。
     */
    @Schema(description = "主键ID")
    private String id;

    /**
     * 知识库ID。
     */
    @Schema(description = "知识库ID")
    private String kbId;

    /**
     * 文档名称。
     */
    @Schema(description = "文档名称")
    private String name;

    /**
     * 来源类型。
     */
    @Schema(description = "来源类型")
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
     * 解析状态。
     */
    @Schema(description = "解析状态")
    private String parseStatus;

    /**
     * 切分状态。
     */
    @Schema(description = "切分状态")
    private String chunkStatus;

    /**
     * 向量状态。
     */
    @Schema(description = "向量状态")
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
     * 由实体转换为返回对象。
     *
     * @param entity 文档实体
     * @return 返回对象
     */
    public static KbDocumentVo from(KbDocument entity) {
        if (entity == null) {
            return null;
        }
        KbDocumentVo vo = new KbDocumentVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
