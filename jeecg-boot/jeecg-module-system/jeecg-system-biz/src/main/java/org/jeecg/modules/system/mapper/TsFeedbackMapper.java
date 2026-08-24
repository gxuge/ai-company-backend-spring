package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsFeedback;
import org.jeecg.modules.system.po.tsfeedback.TsFeedbackQueryPo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackAppendVo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackAttachmentVo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackDetailVo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackListVo;

import java.util.List;

/**
 * 反馈数据访问层。
 */
public interface TsFeedbackMapper extends BaseMapper<TsFeedback> {

    /**
     * 分页查询反馈。
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 反馈分页
     */
    Page<TsFeedbackListVo> selectFeedbackPage(Page<TsFeedbackListVo> page,
                                              @Param("query") TsFeedbackQueryPo query);

    /**
     * 查询反馈详情。
     *
     * @param feedbackId 反馈 ID
     * @param currentUserId 当前登录用户 ID
     * @return 反馈详情
     */
    TsFeedbackDetailVo selectFeedbackDetail(@Param("feedbackId") Long feedbackId,
                                            @Param("currentUserId") String currentUserId);

    /**
     * 查询反馈追加内容。
     *
     * @param feedbackId 反馈 ID
     * @param currentUserId 当前登录用户 ID
     * @return 追加内容列表
     */
    List<TsFeedbackAppendVo> selectFeedbackAppends(@Param("feedbackId") Long feedbackId,
                                                   @Param("currentUserId") String currentUserId);

    /**
     * 查询反馈附件。
     *
     * @param feedbackId 反馈 ID
     * @return 附件列表
     */
    List<TsFeedbackAttachmentVo> selectFeedbackAttachments(@Param("feedbackId") Long feedbackId);

    /**
     * 原子增加反馈点赞数。
     *
     * @param feedbackId 反馈 ID
     * @return 受影响行数
     */
    int incrementLikeCount(@Param("feedbackId") Long feedbackId);

    /**
     * 原子增加反馈评论总数。
     *
     * @param feedbackId 反馈 ID
     * @return 受影响行数
     */
    int incrementCommentCount(@Param("feedbackId") Long feedbackId);

    /**
     * 原子减少反馈评论总数，最低保持为 0。
     *
     * @param feedbackId 反馈 ID
     * @return 受影响行数
     */
    int decrementCommentCount(@Param("feedbackId") Long feedbackId);

    /**
     * 查询反馈最新点赞数。
     *
     * @param feedbackId 反馈 ID
     * @return 点赞数，反馈不存在时返回 null
     */
    Integer selectLikeCount(@Param("feedbackId") Long feedbackId);
}
