package org.jeecg.modules.system.po.tsroleimageprofile;

import lombok.Data;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileQueryDto;
@Data
public class TsRoleImageProfileQueryPo {
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private String userId;
    private Integer pageNo;
    private Integer pageSize;
    private String keyword;
    private String profileName;
    private String styleName;
    private String sourceType;
    private Integer isPublic;
    private Integer status;
    public static TsRoleImageProfileQueryPo fromRequest(String userId, TsRoleImageProfileQueryDto request) {
        TsRoleImageProfileQueryPo po = new TsRoleImageProfileQueryPo();
        po.setUserId(userId);

        if (request == null) {
            po.setPageNo(DEFAULT_PAGE_NO);
            po.setPageSize(DEFAULT_PAGE_SIZE);
            po.setStatus(1);
            return po;
        }

        po.setPageNo(normalizePageNo(request.getPageNo()));
        po.setPageSize(normalizePageSize(request.getPageSize()));
        po.setKeyword(trimToNull(request.getKeyword()));
        po.setProfileName(trimToNull(request.getProfileName()));
        po.setStyleName(trimToNull(request.getStyleName()));
        po.setSourceType(trimToNull(request.getSourceType()));
        po.setIsPublic(request.getIsPublic());
        po.setStatus(request.getStatus());
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