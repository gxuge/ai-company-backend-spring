package org.jeecg.modules.system.po.tsrole;

import lombok.Data;
import org.jeecg.modules.system.dto.tsrole.TsRoleQueryDto;
@Data
public class TsRoleQueryPo {
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private String userId;
    private Integer pageNo;
    private Integer pageSize;
    private String keyword;
    private String gender;
    private Integer status;
    private Integer isPublic;
    public static TsRoleQueryPo fromRequest(String userId, TsRoleQueryDto request) {
        TsRoleQueryPo po = new TsRoleQueryPo();
        po.setUserId(userId);

        if (request == null) {
            po.setPageNo(DEFAULT_PAGE_NO);
            po.setPageSize(DEFAULT_PAGE_SIZE);
            return po;
        }

        po.setPageNo(normalizePageNo(request.getPageNo()));
        po.setPageSize(normalizePageSize(request.getPageSize()));
        po.setKeyword(trimToNull(request.getKeyword()));
        po.setGender(trimToNull(request.getGender()));
        po.setStatus(request.getStatus());
        po.setIsPublic(request.getIsPublic());
        return po;
    }
    private static int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < 1) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }
    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}