package org.jeecg.modules.airag.usage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Usage metric attached to one AI invocation.
 */
@Data
@TableName("ts_ai_usage_metric")
public class AiUsageMetricEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("usage_record_id")
    private Long usageRecordId;
    @TableField("metric_code")
    private String metricCode;
    @TableField("metric_value")
    private BigDecimal metricValue;
    @TableField("metric_unit")
    private String metricUnit;
    @TableField("metric_scope")
    private String metricScope;
    @TableField("ext_json")
    private String extJson;
    @TableField("created_at")
    private Date createdAt;
}
