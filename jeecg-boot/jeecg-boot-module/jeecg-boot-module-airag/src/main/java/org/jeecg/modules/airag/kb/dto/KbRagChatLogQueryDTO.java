package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * RAG 问答日志查询条件。
 */
@Data
@Schema(description = "RAG 问答日志查询条件")
public class KbRagChatLogQueryDTO {
    /**
     * 知识库ID。
     */
    @JsonProperty("kb_id")
    @Schema(description = "知识库ID")
    private String kbId;

    /**
     * 问题。
     */
    @Schema(description = "问题")
    private String query;

    /**
     * 状态。
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 开始时间。
     */
    @JsonProperty("start_time")
    @Schema(description = "开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 结束时间。
     */
    @JsonProperty("end_time")
    @Schema(description = "结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 页码。
     */
    @JsonProperty("page_no")
    @Schema(description = "页码")
    private Integer pageNo;

    /**
     * 每页大小。
     */
    @JsonProperty("page_size")
    @Schema(description = "每页大小")
    private Integer pageSize;
}
