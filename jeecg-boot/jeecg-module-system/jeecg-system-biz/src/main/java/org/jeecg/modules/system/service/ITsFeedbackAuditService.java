package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAuditQueryDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAuditUpdateDto;
import org.jeecg.modules.system.entity.TsFeedbackAuditLog;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackAuditItemVo;

/**
 * 反馈内容审核业务服务。
 */
public interface ITsFeedbackAuditService extends IService<TsFeedbackAuditLog> {

    /**
     * 分页查询统一审核队列。
     *
     * @param request 查询参数
     * @return 审核项分页
     */
    Result<Page<TsFeedbackAuditItemVo>> pageAudits(TsFeedbackAuditQueryDto request);

    /**
     * 审核反馈、评论/回复或追加内容。
     *
     * @param user 当前管理员
     * @param request 审核参数
     * @return 审核结果
     */
    Result<String> auditContent(LoginUser user, TsFeedbackAuditUpdateDto request);
}
