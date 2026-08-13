package org.jeecg.modules.system.vo.tsuserbrowsehistory;

import lombok.Data;
import org.jeecg.modules.system.entity.TsUserBrowseHistory;

import java.util.Date;

/**
 * 用户浏览记录写入结果对象。
 */
@Data
public class TsUserBrowseHistoryRecordVo {

    /** 浏览记录主键。 */
    private Long historyId;

    /** 资源类型：role 角色，story 故事。 */
    private String resourceType;

    /** 角色或故事资源 ID。 */
    private Long resourceId;

    /** 累计浏览次数。 */
    private Long viewCount;

    /** 首次浏览时间。 */
    private Date firstViewedAt;

    /** 最近浏览时间。 */
    private Date lastViewedAt;

    /**
     * 将浏览记录实体转换为写入结果对象。
     *
     * @param entity 浏览记录实体
     * @return 写入结果对象
     */
    public static TsUserBrowseHistoryRecordVo fromEntity(TsUserBrowseHistory entity) {
        if (entity == null) {
            return null;
        }
        TsUserBrowseHistoryRecordVo vo = new TsUserBrowseHistoryRecordVo();
        vo.setHistoryId(entity.getId());
        vo.setResourceType(entity.getResourceType());
        vo.setResourceId(entity.getResourceId());
        vo.setViewCount(entity.getViewCount());
        vo.setFirstViewedAt(entity.getFirstViewedAt());
        vo.setLastViewedAt(entity.getLastViewedAt());
        return vo;
    }
}
