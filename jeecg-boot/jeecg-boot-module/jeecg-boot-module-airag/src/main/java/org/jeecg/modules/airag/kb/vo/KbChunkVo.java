package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * chunk返回对象。
 */
@Data
@Schema(description = "chunk返回对象")
public class KbChunkVo {
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
     * 文档ID。
     */
    @Schema(description = "文档ID")
    private String documentId;

    /**
     * 分段内容。
     */
    @Schema(description = "分段内容")
    private String content;

    /**
     * 分段类型。
     */
    @Schema(description = "分段类型")
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
     * chunk索引列表。
     */
    @Schema(description = "chunk索引列表")
    private List<KbChunkIndexVo> indexList = new ArrayList<>();

    /**
     * 由实体转换为返回对象。
     *
     * @param entity chunk实体
     * @return 返回对象
     */
    public static KbChunkVo from(KbChunk entity) {
        if (entity == null) {
            return null;
        }
        KbChunkVo vo = new KbChunkVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
