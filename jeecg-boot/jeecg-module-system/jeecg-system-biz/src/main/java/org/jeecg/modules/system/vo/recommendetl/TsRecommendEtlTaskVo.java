package org.jeecg.modules.system.vo.recommendetl;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 推荐 ETL 任务展示对象。 */
@Data
public class TsRecommendEtlTaskVo {
    /** 任务 ID。 */
    private Long id;
    /** 任务名称。 */
    private String taskName;
    /** 推荐类型。 */
    private String recommendType;
    /** 时间范围模式。 */
    private String timeRangeMode;
    /** 固定开始时间。 */
    private Date startTime;
    /** 固定结束时间。 */
    private Date endTime;
    /** 最近天数。 */
    private Integer recentDays;
    /** Python 脚本路径。 */
    private String scriptPath;
    /** 输出目录。 */
    private String outputDir;
    /** 存储类型。 */
    private String storageType;
    /** 训练集比例。 */
    private BigDecimal trainRatio;
    /** 评估集比例。 */
    private BigDecimal evalRatio;
    /** 附加参数 JSON。 */
    private String runParamsJson;
    /** Cron 表达式。 */
    private String cronExpression;
    /** 是否启用。 */
    private Integer enabled;
    /** 超时秒数。 */
    private Integer timeoutSeconds;
    /** 当前执行记录 ID。 */
    private Long runningExecutionId;
    /** 最近触发时间。 */
    private Date lastRunAt;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
