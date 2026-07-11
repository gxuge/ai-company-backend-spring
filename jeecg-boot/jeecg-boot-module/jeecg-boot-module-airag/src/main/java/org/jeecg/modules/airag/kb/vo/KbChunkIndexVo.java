package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * chunk索引返回对象。
 */
@Data
@Schema(description = "chunk索引返回对象")
public class KbChunkIndexVo {
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
     * 分段ID。
     */
    @Schema(description = "分段ID")
    private String chunkId;

    /**
     * 索引文本。
     */
    @Schema(description = "索引文本")
    private String indexText;

    /**
     * 索引类型。
     */
    @Schema(description = "索引类型")
    private String indexType;

    /**
     * 向量状态。
     */
    @Schema(description = "向量状态")
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
    private Integer status;

    /**
     * 元数据JSON。
     */
    @Schema(description = "元数据JSON")
    private String metadataJson;

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
     * @param entity chunk索引实体
     * @return 返回对象
     */
    public static KbChunkIndexVo from(KbChunkIndex entity) {
        if (entity == null) {
            return null;
        }
        KbChunkIndexVo vo = new KbChunkIndexVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
