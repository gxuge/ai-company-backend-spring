package org.jeecg.modules.system.po.tschatmessage;

import lombok.Data;
import org.jeecg.modules.system.dto.tschatmessage.TsChatMessageQueryDto;

import java.util.Date;
@Data
public class TsChatMessageQueryPo {
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private String userId;
    private Integer pageNo;
    private Integer pageSize;
    private Long sessionId;
    private String senderType;
    private String messageType;
    private String generateStatus;
    private Long replyToMessageId;
    private String keyword;
    private Date createdAtStart;
    private Date createdAtEnd;
    public static TsChatMessageQueryPo fromRequest(String userId, TsChatMessageQueryDto request) {
        TsChatMessageQueryPo po = new TsChatMessageQueryPo();
        po.setUserId(userId);

        if (request == null) {
            po.setPageNo(DEFAULT_PAGE_NO);
            po.setPageSize(DEFAULT_PAGE_SIZE);
            return po;
        }

        po.setPageNo(normalizePageNo(request.getPageNo()));
        po.setPageSize(normalizePageSize(request.getPageSize()));
        po.setSessionId(request.getSessionId());
        po.setSenderType(trimToNull(request.getSenderType()));
        po.setMessageType(trimToNull(request.getMessageType()));
        po.setGenerateStatus(trimToNull(request.getGenerateStatus()));
        po.setReplyToMessageId(request.getReplyToMessageId());
        po.setKeyword(trimToNull(request.getKeyword()));
        po.setCreatedAtStart(request.getCreatedAtStart());
        po.setCreatedAtEnd(request.getCreatedAtEnd());
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
