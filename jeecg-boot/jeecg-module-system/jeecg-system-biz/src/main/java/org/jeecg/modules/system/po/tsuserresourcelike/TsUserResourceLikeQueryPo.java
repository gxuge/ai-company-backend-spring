package org.jeecg.modules.system.po.tsuserresourcelike;

import lombok.Data;
import org.jeecg.modules.system.dto.tsuserresourcelike.TsUserResourceLikeQueryDto;

/** 用户点赞分页持久化查询参数。 */
@Data
public class TsUserResourceLikeQueryPo {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    /** 当前登录用户 ID。 */
    private String userId;

    /** 归一化后的页码。 */
    private Integer pageNo;

    /** 归一化后的每页数量。 */
    private Integer pageSize;

    /** 资源类型：role 角色，story 故事。 */
    private String resourceType;

    /** 角色名称或故事标题关键字。 */
    private String keyword;

    /**
     * 转换分页查询参数。
     *
     * @param userId 当前登录用户 ID
     * @param request 接口查询参数
     * @return 持久化查询参数
     */
    public static TsUserResourceLikeQueryPo fromRequest(
            String userId, TsUserResourceLikeQueryDto request) {
        TsUserResourceLikeQueryPo po = new TsUserResourceLikeQueryPo();
        po.setUserId(userId);
        po.setPageNo(request == null || request.getPageNo() == null
                || request.getPageNo() < 1 ? DEFAULT_PAGE_NO : request.getPageNo());
        int pageSize = request == null || request.getPageSize() == null
                || request.getPageSize() < 1 ? DEFAULT_PAGE_SIZE : request.getPageSize();
        po.setPageSize(Math.min(pageSize, MAX_PAGE_SIZE));
        po.setResourceType(request == null ? null : trimToNull(request.getResourceType()));
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
