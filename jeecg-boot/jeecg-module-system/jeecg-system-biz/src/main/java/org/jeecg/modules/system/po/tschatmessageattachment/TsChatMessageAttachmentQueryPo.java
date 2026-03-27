package org.jeecg.modules.system.po.tschatmessageattachment;

import lombok.Data;
import org.jeecg.modules.system.dto.tschatmessageattachment.TsChatMessageAttachmentQueryDto;

import java.util.Date;
@Data
public class TsChatMessageAttachmentQueryPo {
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private String userId;
    private Integer pageNo;
    private Integer pageSize;
    private Long messageId;
    private String fileType;
    private String mimeType;
    private String keyword;
    private Date createdAtStart;
    private Date createdAtEnd;
    public static TsChatMessageAttachmentQueryPo fromRequest(String userId, TsChatMessageAttachmentQueryDto request) {
        TsChatMessageAttachmentQueryPo po = new TsChatMessageAttachmentQueryPo();
        po.setUserId(userId);

        if (request == null) {
            po.setPageNo(DEFAULT_PAGE_NO);
            po.setPageSize(DEFAULT_PAGE_SIZE);
            return po;
        }

        po.setPageNo(normalizePageNo(request.getPageNo()));
        po.setPageSize(normalizePageSize(request.getPageSize()));
        po.setMessageId(request.getMessageId());
        po.setFileType(trimToNull(request.getFileType()));
        po.setMimeType(trimToNull(request.getMimeType()));
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