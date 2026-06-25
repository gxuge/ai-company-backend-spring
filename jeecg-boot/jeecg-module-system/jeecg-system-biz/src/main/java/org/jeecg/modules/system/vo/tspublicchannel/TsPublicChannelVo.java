package org.jeecg.modules.system.vo.tspublicchannel;

import lombok.Data;

import java.util.Date;

/**
 * 公开渠道展示对象。
 */
@Data
public class TsPublicChannelVo {
    /** 渠道ID。 */
    private Long id;
    /** 渠道编码。 */
    private String channelCode;
    /** 渠道名称。 */
    private String channelName;
    /** 渠道图片。 */
    private String channelImageUrl;
    /** 目标类型。 */
    private String targetType;
    /** 状态。 */
    private String status;
    /** 排序值。 */
    private Integer sortOrder;
    /** 备注。 */
    private String remark;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
