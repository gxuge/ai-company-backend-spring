package org.jeecg.modules.system.vo.tschatmessage;

import lombok.Data;

import java.util.Date;
@Data
public class TsChatMessageVo {
    private Long id;
    private Long sessionId;
    private String senderType;
    private Long senderId;
    private String senderName;
    private String messageType;
    private String contentText;
    private String contentJson;
    private Long replyToMessageId;
    private Long seqNo;
    private String generateStatus;
    private Date createdAt;
}
