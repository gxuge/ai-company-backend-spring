package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 活动行为进度去重事件。 */
@Data
@Accessors(chain = true)
@TableName("activity_progress_event")
public class TsActivityProgressEvent {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID。 */
    private String userId;
    /** 行为类型。 */
    private String conditionType;
    /** 业务幂等ID。 */
    private String bizId;
    /** 本次增加数量。 */
    private Long countValue;
    /** 创建时间。 */
    private Date createdAt;
}
