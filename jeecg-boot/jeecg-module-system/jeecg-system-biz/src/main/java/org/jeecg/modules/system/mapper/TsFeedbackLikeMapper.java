package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsFeedbackLike;

/**
 * 反馈中心点赞数据访问层。
 */
public interface TsFeedbackLikeMapper extends BaseMapper<TsFeedbackLike> {

    /**
     * 插入点赞记录，唯一键冲突时忽略。
     *
     * @param userId 当前登录用户 ID
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @return 首次点赞返回 1，重复点赞返回 0
     */
    int insertIgnore(@Param("userId") String userId,
                     @Param("targetType") String targetType,
                     @Param("targetId") Long targetId);
}
