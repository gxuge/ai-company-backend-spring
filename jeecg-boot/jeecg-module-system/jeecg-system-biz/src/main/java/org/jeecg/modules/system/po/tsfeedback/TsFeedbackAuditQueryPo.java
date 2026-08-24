package org.jeecg.modules.system.po.tsfeedback;

import lombok.Data;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAuditQueryDto;

/**
 * 反馈内容审核持久化查询参数。
 */
@Data
public class TsFeedbackAuditQueryPo {

    /** 归一化页码。 */
    private Integer pageNo;

    /** 归一化每页数量。 */
    private Integer pageSize;

    /** 审核目标：feedback、comment、append。 */
    private String targetType;

    /** 审核状态：pending、approved、rejected。 */
    private String auditStatus;

    /** 标题、内容或发布用户关键字。 */
    private String keyword;

    /**
     * 将接口参数转换为持久化查询参数。
     *
     * @param request 查询参数
     * @return 持久化查询参数
     */
    public static TsFeedbackAuditQueryPo fromRequest(TsFeedbackAuditQueryDto request) {
        TsFeedbackAuditQueryPo po = new TsFeedbackAuditQueryPo();
        po.setPageNo(request == null || request.getPageNo() == null ? 1 : request.getPageNo());
        po.setPageSize(request == null || request.getPageSize() == null
                ? 10 : Math.min(request.getPageSize(), 100));
        po.setTargetType(trimToNull(request == null ? null : request.getTargetType()));
        po.setAuditStatus(trimToNull(request == null ? null : request.getAuditStatus()));
        po.setKeyword(trimToNull(request == null ? null : request.getKeyword()));
        return po;
    }

    /**
     * 去除首尾空白并将空字符串转换为 null。
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
