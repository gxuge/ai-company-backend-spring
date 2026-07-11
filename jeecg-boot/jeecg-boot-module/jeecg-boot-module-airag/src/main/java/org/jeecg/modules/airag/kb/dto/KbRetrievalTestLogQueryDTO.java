package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Date;

/**
 * 检索测试日志查询请求。
 */
@Data
@Schema(description = "检索测试日志查询请求")
public class KbRetrievalTestLogQueryDTO {
    /**
     * 知识库ID。
     */
    @JsonProperty("kb_id")
    @Schema(description = "知识库ID")
    private String kbId;

    /**
     * query关键字。
     */
    @JsonProperty("query")
    @Schema(description = "query关键字")
    private String query;

    /**
     * 状态。
     */
    @Pattern(regexp = "^(success|failed)?$", message = "status只能是success或failed")
    @JsonProperty("status")
    @Schema(description = "状态：success/failed")
    private String status;

    /**
     * 开始时间。
     */
    @JsonProperty("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "开始时间")
    private Date startTime;

    /**
     * 结束时间。
     */
    @JsonProperty("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "结束时间")
    private Date endTime;

    /**
     * 页码。
     */
    @Min(value = 1, message = "page_no必须大于0")
    @JsonProperty("page_no")
    @Schema(description = "页码")
    private Integer pageNo;

    /**
     * 每页大小。
     */
    @Min(value = 1, message = "page_size必须大于0")
    @Max(value = 100, message = "page_size不能超过100")
    @JsonProperty("page_size")
    @Schema(description = "每页大小")
    private Integer pageSize;
}
