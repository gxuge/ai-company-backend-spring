package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 广告曝光或点击事件。 */
@Data
@Accessors(chain = true)
@TableName("ts_ad_event")
public class TsAdEvent {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 客户端事件幂等ID。 */
    private String eventId;
    /** 广告内容ID。 */
    private Long contentId;
    /** 广告位编码快照。 */
    private String slotCode;
    /** 事件类型：IMPRESSION/CLICK。 */
    private String eventType;
    /** 登录用户ID。 */
    private String userId;
    /** 匿名访客ID。 */
    private String visitorId;
    /** 平台：WEB/IOS/ANDROID。 */
    private String platform;
    /** 事件发生时间。 */
    private Date occurredAt;
    /** 入库时间。 */
    private Date createdAt;
}
