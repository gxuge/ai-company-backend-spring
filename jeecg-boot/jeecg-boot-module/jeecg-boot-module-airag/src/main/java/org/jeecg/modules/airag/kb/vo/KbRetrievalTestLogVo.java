package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.airag.kb.entity.KbRetrievalTestLog;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 检索测试日志返回对象。
 */
@Data
@Schema(description = "检索测试日志返回对象")
public class KbRetrievalTestLogVo {
    /**
     * 主键ID。
     */
    @Schema(description = "主键ID")
    private String id;

    /**
     * 知识库ID。
     */
    @Schema(description = "知识库ID")
    @JsonProperty("kb_id")
    private String kbId;

    /**
     * 原始query。
     */
    @Schema(description = "原始query")
    private String query;

    /**
     * 优化query JSON。
     */
    @Schema(description = "优化query JSON")
    @JsonProperty("optimized_queries_json")
    private String optimizedQueriesJson;

    /**
     * 实际使用query JSON。
     */
    @Schema(description = "实际使用query JSON")
    @JsonProperty("used_queries_json")
    private String usedQueriesJson;

    /**
     * 检索模式。
     */
    @Schema(description = "检索模式")
    @JsonProperty("search_mode")
    private String searchMode;

    /**
     * 请求与实际参数JSON。
     */
    @Schema(description = "请求与实际参数JSON")
    @JsonProperty("params_json")
    private String paramsJson;

    /**
     * 返回条数。
     */
    @Schema(description = "返回条数")
    @JsonProperty("result_count")
    private Integer resultCount;

    /**
     * 结果JSON。
     */
    @Schema(description = "结果JSON")
    @JsonProperty("result_json")
    private String resultJson;

    /**
     * 调试JSON。
     */
    @Schema(description = "调试JSON")
    @JsonProperty("debug_json")
    private String debugJson;

    /**
     * 状态。
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 错误信息。
     */
    @Schema(description = "错误信息")
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("created_at")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("updated_at")
    private Date updatedAt;

    /**
     * 由实体转换。
     *
     * @param entity 日志实体
     * @return 返回对象
     */
    public static KbRetrievalTestLogVo from(KbRetrievalTestLog entity) {
        if (entity == null) {
            return null;
        }
        KbRetrievalTestLogVo vo = new KbRetrievalTestLogVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
