package org.jeecg.modules.system.po.tsroleimagegeneraterecord;

import lombok.Data;
import org.jeecg.modules.system.dto.tsroleimagegeneraterecord.TsRoleImageGenerateRecordQueryDto;
@Data
public class TsRoleImageGenerateRecordQueryPo {
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private String userId;
    private Integer pageNo;
    private Integer pageSize;
    private String keyword;
    private Long roleId;
    private String generateStatus;
    private String applyStatus;
    private String requestId;
    public static TsRoleImageGenerateRecordQueryPo fromRequest(String userId, TsRoleImageGenerateRecordQueryDto request) {
        TsRoleImageGenerateRecordQueryPo po = new TsRoleImageGenerateRecordQueryPo();
        po.setUserId(userId);

        if (request == null) {
            po.setPageNo(DEFAULT_PAGE_NO);
            po.setPageSize(DEFAULT_PAGE_SIZE);
            return po;
        }

        po.setPageNo(normalizePageNo(request.getPageNo()));
        po.setPageSize(normalizePageSize(request.getPageSize()));
        po.setKeyword(trimToNull(request.getKeyword()));
        po.setRoleId(request.getRoleId());
        po.setGenerateStatus(trimToNull(request.getGenerateStatus()));
        po.setApplyStatus(trimToNull(request.getApplyStatus()));
        po.setRequestId(trimToNull(request.getRequestId()));
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