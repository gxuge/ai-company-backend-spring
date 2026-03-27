package org.jeecg.modules.system.po.tschatsession;

import lombok.Data;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionQueryDto;

import java.util.Date;
@Data
public class TsChatSessionQueryPo {
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private String userId;
    private Integer pageNo;
    private Integer pageSize;
    private String sessionType;
    private Integer sessionStatus;
    private Long targetRoleId;
    private Long storyId;
    private String keyword;
    private Date lastMessageAtStart;
    private Date lastMessageAtEnd;
    public static TsChatSessionQueryPo fromRequest(String userId, TsChatSessionQueryDto request) {
        TsChatSessionQueryPo po = new TsChatSessionQueryPo();
        po.setUserId(userId);

        if (request == null) {
            po.setPageNo(DEFAULT_PAGE_NO);
            po.setPageSize(DEFAULT_PAGE_SIZE);
            return po;
        }

        po.setPageNo(normalizePageNo(request.getPageNo()));
        po.setPageSize(normalizePageSize(request.getPageSize()));
        po.setSessionType(trimToNull(request.getSessionType()));
        po.setSessionStatus(request.getSessionStatus());
        po.setTargetRoleId(request.getTargetRoleId());
        po.setStoryId(request.getStoryId());
        po.setKeyword(trimToNull(request.getKeyword()));
        po.setLastMessageAtStart(request.getLastMessageAtStart());
        po.setLastMessageAtEnd(request.getLastMessageAtEnd());
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
