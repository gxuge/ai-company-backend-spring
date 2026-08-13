package org.jeecg.modules.system.po.tsfeedback;

import lombok.Data;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentQueryDto;
import org.jeecg.modules.system.util.tsfeedback.TsFeedbackConstants;

/**
 * 反馈评论分页持久化查询参数。
 */
@Data
public class TsFeedbackCommentQueryPo {

    /** 当前登录用户 ID，用于返回点赞状态。 */
    private String currentUserId;

    /** 所属反馈 ID。 */
    private Long feedbackId;

    /** 一级评论 ID，查询全部二级回复时使用。 */
    private Long parentId;

    /** 归一化页码。 */
    private Integer pageNo;

    /** 归一化每页数量。 */
    private Integer pageSize;

    /** 排序方式：latest 或 hot。 */
    private String sort;

    /**
     * 构造评论分页查询参数。
     *
     * @param currentUserId 当前登录用户 ID
     * @param feedbackId 反馈 ID
     * @param parentId 一级评论 ID
     * @param request 查询参数
     * @return 持久化查询参数
     */
    public static TsFeedbackCommentQueryPo fromRequest(String currentUserId,
                                                       Long feedbackId,
                                                       Long parentId,
                                                       TsFeedbackCommentQueryDto request) {
        TsFeedbackCommentQueryPo po = new TsFeedbackCommentQueryPo();
        po.setCurrentUserId(currentUserId);
        po.setFeedbackId(feedbackId);
        po.setParentId(parentId);
        po.setPageNo(request == null || request.getPageNo() == null ? 1 : request.getPageNo());
        po.setPageSize(request == null || request.getPageSize() == null ? 10 : Math.min(request.getPageSize(), 100));
        String sort = request == null ? null : request.getSort();
        po.setSort(sort == null || sort.isBlank() ? TsFeedbackConstants.SORT_LATEST : sort.trim());
        return po;
    }
}
