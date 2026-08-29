package org.jeecg.modules.system.dto.recommendetl;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 推荐 ETL 任务保存参数。 */
@Data
public class TsRecommendEtlTaskSaveDto {
    /** 更新时的任务 ID，新增时为空。 */
    private Long id;
    /** 任务名称。 */
    @NotBlank
    @Size(max = 100)
    private String taskName;
    /** 推荐类型：ROLE/STORY。 */
    @NotBlank
    private String recommendType;
    /** 时间范围模式：FIXED/RECENT_DAYS。 */
    @NotBlank
    private String timeRangeMode;
    /** 固定范围开始时间。 */
    private Date startTime;
    /** 固定范围结束时间。 */
    private Date endTime;
    /** 最近天数。 */
    @Min(1)
    @Max(3650)
    private Integer recentDays;
    /** Python 脚本路径。 */
    @NotBlank
    @Size(max = 1000)
    private String scriptPath;
    /** 输出目录。 */
    @NotBlank
    @Size(max = 1000)
    private String outputDir;
    /** 存储类型：LOCAL/OSS。 */
    @NotBlank
    private String storageType;
    /** 训练集比例。 */
    @DecimalMin("0.00001")
    @DecimalMax("0.99999")
    private BigDecimal trainRatio;
    /** 评估集比例。 */
    @DecimalMin("0.00001")
    @DecimalMax("0.99999")
    private BigDecimal evalRatio;
    /** 附加运行参数 JSON。 */
    private String runParamsJson;
    /** Quartz Cron 表达式。 */
    @Size(max = 100)
    private String cronExpression;
    /** 是否启用：0否/1是。 */
    private Integer enabled;
    /** 执行超时秒数。 */
    @Min(10)
    private Integer timeoutSeconds;
}
