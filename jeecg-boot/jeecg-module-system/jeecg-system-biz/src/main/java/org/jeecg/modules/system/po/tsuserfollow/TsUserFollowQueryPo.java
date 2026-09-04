package org.jeecg.modules.system.po.tsuserfollow;

import lombok.Data;
import org.jeecg.modules.system.dto.tsuserfollow.TsUserFollowQueryDto;

/** 用户关注分页持久化查询参数。 */
@Data
public class TsUserFollowQueryPo {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    /** 当前登录用户 ID。 */
    private String userId;

    /** 归一化后的页码。 */
    private Integer pageNo;

    /** 归一化后的每页数量。 */
    private Integer pageSize;

    /** 用户账号或名称关键字。 */
    private String keyword;

    /**
     * 转换分页查询参数。
     *
     * @param userId 当前登录用户 ID
     * @param request 接口查询参数
     * @return 持久化查询参数
     */
    public static TsUserFollowQueryPo fromRequest(
            String userId, TsUserFollowQueryDto request) {
        TsUserFollowQueryPo po = new TsUserFollowQueryPo();
        po.setUserId(userId);
        po.setPageNo(request == null || request.getPageNo() == null
                || request.getPageNo() < 1 ? DEFAULT_PAGE_NO : request.getPageNo());
        int pageSize = request == null || request.getPageSize() == null
                || request.getPageSize() < 1 ? DEFAULT_PAGE_SIZE : request.getPageSize();
        po.setPageSize(Math.min(pageSize, MAX_PAGE_SIZE));
        po.setKeyword(request == null ? null : trimToNull(request.getKeyword()));
        return po;
    }

    /** 去除首尾空白并将空字符串转换为 null。 */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
