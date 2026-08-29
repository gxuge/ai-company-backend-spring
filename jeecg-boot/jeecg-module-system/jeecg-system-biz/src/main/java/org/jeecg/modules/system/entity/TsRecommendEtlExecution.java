package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 推荐训练数据 ETL 执行记录。 */
@Data
@Accessors(chain = true)
@TableName("ts_recommend_etl_execution")
public class TsRecommendEtlExecution {
    /** 主键。 */
    @TableId(type = IdType.INPUT)
    private Long id;
    /** 任务 ID。 */
    private Long taskId;
    /** 任务名称快照。 */
    private String taskName;
    /** 推荐类型：ROLE/STORY。 */
    private String recommendType;
    /** 触发类型：MANUAL/SCHEDULED。 */
    private String triggerType;
    /** 状态：WAITING/RUNNING/SUCCESS/FAILED。 */
    private String status;
    /** 数据开始时间。 */
    private Date rangeStartTime;
    /** 数据结束时间。 */
    private Date rangeEndTime;
    /** 执行参数快照 JSON。 */
    private String argumentsJson;
    /** 进程开始时间。 */
    private Date startedAt;
    /** 进程结束时间。 */
    private Date finishedAt;
    /** 执行耗时毫秒。 */
    private Long durationMs;
    /** Python 退出码。 */
    private Integer processExitCode;
    /** 训练集数量。 */
    private Long trainCount;
    /** 评估集数量。 */
    private Long evalCount;
    /** 正样本数量。 */
    private Long positiveCount;
    /** 负样本数量。 */
    private Long negativeCount;
    /** 训练集文件路径。 */
    private String trainPath;
    /** 评估集文件路径。 */
    private String evalPath;
    /** Python 结果 JSON。 */
    private String resultJson;
    /** 完整日志文件路径。 */
    private String logPath;
    /** 截断后的运行日志。 */
    private String logContent;
    /** 机器错误码。 */
    private String errorCode;
    /** 错误信息。 */
    private String errorMessage;
    /** 创建人。 */
    private String createBy;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
