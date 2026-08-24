package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsFeedback;
import org.jeecg.modules.system.entity.TsFeedbackAppend;
import org.jeecg.modules.system.entity.TsFeedbackAuditLog;
import org.jeecg.modules.system.entity.TsFeedbackComment;
import org.jeecg.modules.system.po.tsfeedback.TsFeedbackAuditQueryPo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackAuditItemVo;

/**
 * 反馈内容审核数据访问层。
 */
public interface TsFeedbackAuditMapper extends BaseMapper<TsFeedbackAuditLog> {

    /**
     * 分页查询统一审核队列。
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 审核项分页
     */
    Page<TsFeedbackAuditItemVo> selectAuditPage(Page<TsFeedbackAuditItemVo> page,
                                               @Param("query") TsFeedbackAuditQueryPo query);

    /**
     * 锁定待审核反馈，避免并发重复审核。
     *
     * @param targetId 反馈 ID
     * @return 反馈实体
     */
    TsFeedback selectFeedbackForUpdate(@Param("targetId") Long targetId);

    /**
     * 锁定待审核评论或回复，避免并发重复审核。
     *
     * @param targetId 评论 ID
     * @return 评论实体
     */
    TsFeedbackComment selectCommentForUpdate(@Param("targetId") Long targetId);

    /**
     * 锁定待审核追加内容，避免并发重复审核。
     *
     * @param targetId 追加内容 ID
     * @return 追加内容实体
     */
    TsFeedbackAppend selectAppendForUpdate(@Param("targetId") Long targetId);
}
