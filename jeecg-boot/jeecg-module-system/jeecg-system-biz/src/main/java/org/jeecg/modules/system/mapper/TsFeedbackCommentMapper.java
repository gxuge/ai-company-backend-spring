package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsFeedbackComment;
import org.jeecg.modules.system.po.tsfeedback.TsFeedbackCommentQueryPo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackCommentVo;

import java.util.List;

/**
 * 反馈评论数据访问层。
 */
public interface TsFeedbackCommentMapper extends BaseMapper<TsFeedbackComment> {

    /**
     * 分页查询一级评论。
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 一级评论分页
     */
    Page<TsFeedbackCommentVo> selectFirstLevelCommentPage(Page<TsFeedbackCommentVo> page,
                                                         @Param("query") TsFeedbackCommentQueryPo query);

    /**
     * 批量查询一级评论的前若干条回复。
     *
     * @param parentIds 一级评论 ID 集合
     * @param currentUserId 当前登录用户 ID
     * @param previewSize 每条一级评论预览回复数量
     * @return 回复预览列表
     */
    List<TsFeedbackCommentVo> selectReplyPreviews(@Param("parentIds") List<Long> parentIds,
                                                  @Param("currentUserId") String currentUserId,
                                                  @Param("previewSize") Integer previewSize);

    /**
     * 分页查询一级评论的全部回复。
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 二级回复分页
     */
    Page<TsFeedbackCommentVo> selectReplyPage(Page<TsFeedbackCommentVo> page,
                                              @Param("query") TsFeedbackCommentQueryPo query);

    /**
     * 原子增加评论点赞数。
     *
     * @param commentId 评论 ID
     * @return 受影响行数
     */
    int incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 查询评论最新点赞数。
     *
     * @param commentId 评论 ID
     * @return 点赞数，评论不存在时返回 null
     */
    Integer selectLikeCount(@Param("commentId") Long commentId);
}
