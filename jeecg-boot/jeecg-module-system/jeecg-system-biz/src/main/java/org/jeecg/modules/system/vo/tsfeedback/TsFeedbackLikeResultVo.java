package org.jeecg.modules.system.vo.tsfeedback;

import lombok.Data;

/**
 * 反馈中心点赞结果。
 */
@Data
public class TsFeedbackLikeResultVo {

    /** 点赞目标类型：feedback 或 comment。 */
    private String targetType;

    /** 点赞目标 ID。 */
    private Long targetId;

    /** 是否已点赞。 */
    private Boolean liked;

    /** 最新点赞数量。 */
    private Integer likeCount;

    /**
     * 创建点赞结果。
     *
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @param likeCount 最新点赞数量
     * @return 点赞结果
     */
    public static TsFeedbackLikeResultVo liked(String targetType, Long targetId, Integer likeCount) {
        TsFeedbackLikeResultVo vo = new TsFeedbackLikeResultVo();
        vo.setTargetType(targetType);
        vo.setTargetId(targetId);
        vo.setLiked(Boolean.TRUE);
        vo.setLikeCount(likeCount);
        return vo;
    }
}
