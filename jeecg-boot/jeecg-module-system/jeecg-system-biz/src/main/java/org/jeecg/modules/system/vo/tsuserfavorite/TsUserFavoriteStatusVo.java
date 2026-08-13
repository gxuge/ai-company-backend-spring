package org.jeecg.modules.system.vo.tsuserfavorite;

import lombok.Data;

/**
 * 用户收藏状态对象。
 */
@Data
public class TsUserFavoriteStatusVo {

    /** 资源类型：role 角色，story 故事。 */
    private String resourceType;

    /** 角色或故事资源 ID。 */
    private Long resourceId;

    /** 是否已被当前用户收藏。 */
    private Boolean favorited;

    /**
     * 创建收藏状态对象。
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param favorited 是否已收藏
     * @return 收藏状态对象
     */
    public static TsUserFavoriteStatusVo of(String resourceType, Long resourceId, boolean favorited) {
        TsUserFavoriteStatusVo vo = new TsUserFavoriteStatusVo();
        vo.setResourceType(resourceType);
        vo.setResourceId(resourceId);
        vo.setFavorited(favorited);
        return vo;
    }
}
