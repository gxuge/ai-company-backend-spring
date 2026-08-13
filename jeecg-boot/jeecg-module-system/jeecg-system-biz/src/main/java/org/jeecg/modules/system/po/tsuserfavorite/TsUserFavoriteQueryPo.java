package org.jeecg.modules.system.po.tsuserfavorite;

import lombok.Data;
import org.jeecg.modules.system.dto.tsuserfavorite.TsUserFavoriteQueryDto;

/**
 * 用户收藏分页查询持久化参数。
 */
@Data
public class TsUserFavoriteQueryPo {

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
     * 将接口查询参数转换为持久化查询参数。
     *
     * @param userId 当前登录用户 ID
     * @param request 查询参数
     * @return 持久化查询参数
     */
    public static TsUserFavoriteQueryPo fromRequest(String userId, TsUserFavoriteQueryDto request) {
        TsUserFavoriteQueryPo po = new TsUserFavoriteQueryPo();
        po.setUserId(userId);
        if (request == null) {
            po.setPageNo(DEFAULT_PAGE_NO);
            po.setPageSize(DEFAULT_PAGE_SIZE);
            return po;
        }
        po.setPageNo(normalizePageNo(request.getPageNo()));
        po.setPageSize(normalizePageSize(request.getPageSize()));
        po.setResourceType(trimToNull(request.getResourceType()));
        po.setKeyword(trimToNull(request.getKeyword()));
        return po;
    }

    /**
     * 归一化页码。
     *
     * @param pageNo 原始页码
     * @return 至少为 1 的页码
     */
    private static int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? DEFAULT_PAGE_NO : pageNo;
    }

    /**
     * 归一化每页数量并限制最大值。
     *
     * @param pageSize 原始每页数量
     * @return 1 到 100 之间的每页数量
     */
    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
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
