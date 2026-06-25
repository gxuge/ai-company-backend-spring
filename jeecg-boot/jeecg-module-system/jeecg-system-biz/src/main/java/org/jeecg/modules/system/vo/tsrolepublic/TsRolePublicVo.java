package org.jeecg.modules.system.vo.tsrolepublic;

import lombok.Data;

import java.util.Date;

/**
 * 角色公开记录展示对象。
 */
@Data
public class TsRolePublicVo {
    /** 公开记录ID。 */
    private Long id;
    /** 角色ID。 */
    private Long roleId;
    /** 角色名称。 */
    private String roleName;
    /** 所属用户ID。 */
    private String ownerUserId;
    /** 所属用户显示名。 */
    private String ownerDisplayName;
    /** 渠道编码。 */
    private String channelCode;
    /** 渠道名称。 */
    private String channelName;
    /** 状态。 */
    private String status;
    /** 展示标题。 */
    private String displayTitle;
    /** 展示副标题。 */
    private String displaySubtitle;
    /** 封面图。 */
    private String coverImageUrl;
    /** 展示简介。 */
    private String introText;
    /** 排序值。 */
    private Integer sortOrder;
    /** 上架时间。 */
    private Date publishedAt;
    /** 下架时间。 */
    private Date offlineAt;
    /** 驳回原因。 */
    private String rejectReason;
    /** 扩展JSON。 */
    private String extJson;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
