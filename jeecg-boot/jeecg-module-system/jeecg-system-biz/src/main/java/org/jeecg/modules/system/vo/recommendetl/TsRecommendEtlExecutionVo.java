package org.jeecg.modules.system.vo.recommendetl;

import lombok.Data;

import java.util.Date;

/** 推荐 ETL 执行记录展示对象。 */
@Data
public class TsRecommendEtlExecutionVo {
    /** 执行记录 ID。 */
    private Long id;
    /** 任务 ID。 */
    private Long taskId;
    /** 任务名称。 */
    private String taskName;
    /** 推荐类型。 */
    private String recommendType;
    /** 触发类型。 */
    private String triggerType;
    /** 执行状态。 */
    private String status;
    /** 数据开始时间。 */
    private Date rangeStartTime;
    /** 数据结束时间。 */
    private Date rangeEndTime;
    /** 参数快照 JSON。 */
    private String argumentsJson;
    /** 开始时间。 */
    private Date startedAt;
    /** 结束时间。 */
    private Date finishedAt;
    /** 耗时毫秒。 */
    private Long durationMs;
    /** 进程退出码。 */
    private Integer processExitCode;
    /** train 数量。 */
    private Long trainCount;
    /** eval 数量。 */
    private Long evalCount;
    /** 正样本数量。 */
    private Long positiveCount;
    /** 负样本数量。 */
    private Long negativeCount;
    /** train 文件路径。 */
    private String trainPath;
    /** eval 文件路径。 */
    private String evalPath;
    /** Python 结果 JSON。 */
    private String resultJson;
    /** 完整日志路径。 */
    private String logPath;
    /** 截断日志内容。 */
    private String logContent;
    /** 错误码。 */
    private String errorCode;
    /** 错误信息。 */
    private String errorMessage;
    /** 创建时间。 */
    private Date createTime;
}
