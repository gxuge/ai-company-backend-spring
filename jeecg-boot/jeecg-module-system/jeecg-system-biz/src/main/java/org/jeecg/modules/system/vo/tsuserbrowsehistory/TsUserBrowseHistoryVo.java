package org.jeecg.modules.system.vo.tsuserbrowsehistory;

import lombok.Data;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceVo;

import java.util.Date;
import java.util.Map;

/**
 * 用户浏览记录展示对象。
 */
@Data
public class TsUserBrowseHistoryVo {

    /** 浏览记录主键。 */
    private Long historyId;

    /** 资源类型：role 角色，story 故事。 */
    private String resourceType;

    /** 角色或故事资源 ID。 */
    private Long resourceId;

    /** 当前在线公开记录 ID。 */
    private Long publicId;

    /** 当前在线公开渠道编码。 */
    private String channelCode;

    /** 角色名称或故事标题。 */
    private String title;

    /** 角色副标题或故事展示副标题。 */
    private String subtitle;

    /** 角色或故事简介。 */
    private String description;

    /** 封面图片地址。 */
    private String coverUrl;

    /** 角色头像地址。 */
    private String avatarUrl;

    /** 故事场景图片地址。 */
    private String sceneImageUrl;

    /** 角色性别。 */
    private String gender;

    /** 角色职业。 */
    private String occupation;

    /** 故事模式。 */
    private String storyMode;

    /** 故事关注人数。 */
    private Long followerCount;

    /** 故事对话人数。 */
    private Long dialogueCount;

    /** 作者名称。 */
    private String authorName;

    /** 作者头像。 */
    private String authorAvatar;

    /** 资源更新时间。 */
    private Date resourceUpdatedAt;

    /** 累计浏览次数。 */
    private Long viewCount;

    /** 首次浏览时间。 */
    private Date firstViewedAt;

    /** 最近浏览时间。 */
    private Date lastViewedAt;

    /** 统一图片语义资源。 */
    private Map<String, TsImageResourceVo> imageResources;
}
