package org.jeecg.modules.system.vo.tsuserresourcelike;

import lombok.Data;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceVo;

import java.util.Date;
import java.util.Map;

/** 用户点赞资源列表展示对象。 */
@Data
public class TsUserResourceLikeVo {

    /** 点赞关系主键。 */
    private Long likeId;

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

    /** 封面图片地址。 */
    private String coverUrl;

    /** 角色头像地址。 */
    private String avatarUrl;

    /** 故事场景图片地址。 */
    private String sceneImageUrl;

    /** 作者名称。 */
    private String authorName;

    /** 作者头像。 */
    private String authorAvatar;

    /** 点赞时间。 */
    private Date likedAt;

    /** 统一图片语义资源。 */
    private Map<String, TsImageResourceVo> imageResources;
}
