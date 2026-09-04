package org.jeecg.modules.system.vo.tsuserresourcelike;

import lombok.Data;

/** 角色或故事点赞状态对象。 */
@Data
public class TsUserResourceLikeStatusVo {

    /** 资源类型：role 角色，story 故事。 */
    private String resourceType;

    /** 角色或故事资源 ID。 */
    private Long resourceId;

    /** 当前登录用户是否已点赞。 */
    private Boolean liked;

    /** 当前有效点赞总数。 */
    private Long likeCount;

    /** 创建点赞状态对象。 */
    public static TsUserResourceLikeStatusVo of(
            String resourceType, Long resourceId, boolean liked, long likeCount) {
        TsUserResourceLikeStatusVo vo = new TsUserResourceLikeStatusVo();
        vo.setResourceType(resourceType);
        vo.setResourceId(resourceId);
        vo.setLiked(liked);
        vo.setLikeCount(likeCount);
        return vo;
    }
}
