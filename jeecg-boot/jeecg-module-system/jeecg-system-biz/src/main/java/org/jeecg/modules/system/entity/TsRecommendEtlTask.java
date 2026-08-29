package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/** 推荐训练数据 ETL 任务。 */
@Data
@Accessors(chain = true)
@TableName("ts_recommend_etl_task")
public class TsRecommendEtlTask {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 任务名称。 */
    private String taskName;
    /** 推荐类型：ROLE/STORY。 */
    private String recommendType;
    /** 时间范围模式：FIXED/RECENT_DAYS。 */
    private String timeRangeMode;
    /** 固定范围开始时间。 */
    private Date startTime;
    /** 固定范围结束时间。 */
    private Date endTime;
    /** 最近天数。 */
    private Integer recentDays;
    /** Python 脚本路径。 */
    private String scriptPath;
    /** 输出目录。 */
    private String outputDir;
    /** 存储类型：LOCAL/OSS。 */
    private String storageType;
    /** 训练集比例。 */
    private BigDecimal trainRatio;
    /** 评估集比例。 */
    private BigDecimal evalRatio;
    /** 附加运行参数 JSON。 */
    private String runParamsJson;
    /** Quartz Cron 表达式。 */
    private String cronExpression;
    /** 是否启用：0否/1是。 */
    private Integer enabled;
    /** 执行超时秒数。 */
    private Integer timeoutSeconds;
    /** 当前运行记录 ID。 */
    private Long runningExecutionId;
    /** 最近触发时间。 */
    private Date lastRunAt;
    /** 创建人。 */
    private String createBy;
    /** 创建时间。 */
    private Date createTime;
    /** 更新人。 */
    private String updateBy;
    /** 更新时间。 */
    private Date updateTime;
    /** 逻辑删除：0正常/1删除。 */
    @TableLogic
    private Integer delFlag;
}
