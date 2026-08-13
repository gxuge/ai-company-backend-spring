package org.jeecg.modules.system.po.tsfeedback;

import lombok.Data;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackQueryDto;
import org.jeecg.modules.system.util.tsfeedback.TsFeedbackConstants;

/**
 * 反馈分页持久化查询参数。
 */
@Data
public class TsFeedbackQueryPo {

    /** 当前登录用户 ID，用于返回点赞状态。 */
    private String currentUserId;

    /** 反馈发布用户 ID，仅“我的反馈”查询使用。 */
    private String ownerUserId;

    /** 归一化页码。 */
    private Integer pageNo;

    /** 归一化每页数量。 */
    private Integer pageSize;

    /** 反馈类型。 */
    private String type;

    /** 反馈状态。 */
    private String status;

    /** 排序方式：latest 或 hot。 */
    private String sort;

    /** 标题或内容关键字。 */
    private String keyword;

    /**
     * 将接口参数转换为持久化查询参数。
     *
     * @param currentUserId 当前登录用户 ID
     * @param ownerUserId 反馈发布用户 ID
     * @param request 查询参数
     * @return 持久化查询参数
     */
    public static TsFeedbackQueryPo fromRequest(String currentUserId,
                                                String ownerUserId,
                                                TsFeedbackQueryDto request) {
        TsFeedbackQueryPo po = new TsFeedbackQueryPo();
        po.setCurrentUserId(currentUserId);
        po.setOwnerUserId(ownerUserId);
        po.setPageNo(request == null || request.getPageNo() == null ? 1 : request.getPageNo());
        po.setPageSize(request == null || request.getPageSize() == null ? 10 : Math.min(request.getPageSize(), 100));
        po.setType(trimToNull(request == null ? null : request.getType()));
        po.setStatus(trimToNull(request == null ? null : request.getStatus()));
        po.setSort(defaultSort(request == null ? null : request.getSort()));
        po.setKeyword(trimToNull(request == null ? null : request.getKeyword()));
        return po;
    }

    /**
     * 归一化排序方式。
     *
     * @param sort 原始排序方式
     * @return 排序方式
     */
    private static String defaultSort(String sort) {
        String normalized = trimToNull(sort);
        return normalized == null ? TsFeedbackConstants.SORT_LATEST : normalized;
    }

    /**
     * 去除字符串首尾空白并将空字符串转换为 null。
     *
     * @param value 原始字符串
     * @return 归一化字符串
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
