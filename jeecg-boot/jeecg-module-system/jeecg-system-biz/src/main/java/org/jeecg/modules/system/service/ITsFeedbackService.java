package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAppendCreateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCreateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackQueryDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackStatusUpdateDto;
import org.jeecg.modules.system.entity.TsFeedback;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackDetailVo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackListVo;

/**
 * 反馈业务服务。
 */
public interface ITsFeedbackService extends IService<TsFeedback> {

    /**
     * 发布反馈。
     *
     * @param user 当前登录用户
     * @param request 发布参数
     * @return 新反馈 ID
     */
    Result<Long> createFeedback(LoginUser user, TsFeedbackCreateDto request);

    /**
     * 分页查询全部反馈。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 反馈分页
     */
    Result<Page<TsFeedbackListVo>> pageFeedbacks(LoginUser user, TsFeedbackQueryDto request);

    /**
     * 查询反馈详情。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @return 反馈详情
     */
    Result<TsFeedbackDetailVo> getFeedback(LoginUser user, Long feedbackId);

    /**
     * 分页查询当前用户反馈。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 我的反馈分页
     */
    Result<Page<TsFeedbackListVo>> pageMyFeedbacks(LoginUser user, TsFeedbackQueryDto request);

    /**
     * 追加反馈，仅反馈发起人可操作。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @param request 追加参数
     * @return 追加记录 ID
     */
    Result<Long> appendFeedback(LoginUser user, Long feedbackId, TsFeedbackAppendCreateDto request);

    /**
     * 管理端更新反馈状态。
     *
     * @param user 当前管理员
     * @param feedbackId 反馈 ID
     * @param request 状态参数
     * @return 更新结果
     */
    Result<String> updateFeedbackStatus(LoginUser user,
                                        Long feedbackId,
                                        TsFeedbackStatusUpdateDto request);
}
