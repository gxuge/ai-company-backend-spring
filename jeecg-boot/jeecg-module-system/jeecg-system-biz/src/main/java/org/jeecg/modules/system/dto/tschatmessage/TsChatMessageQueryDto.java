package org.jeecg.modules.system.dto.tschatmessage;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
@Data
public class TsChatMessageQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private Long sessionId;
    private String senderType;
    private String messageType;
    private String generateStatus;
    private Long replyToMessageId;
    private String keyword;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAtStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAtEnd;
}
