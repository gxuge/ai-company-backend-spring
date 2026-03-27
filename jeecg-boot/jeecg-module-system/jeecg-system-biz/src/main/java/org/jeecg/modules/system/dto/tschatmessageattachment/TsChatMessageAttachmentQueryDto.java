package org.jeecg.modules.system.dto.tschatmessageattachment;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
@Data
public class TsChatMessageAttachmentQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private Long messageId;
    private String fileType;
    private String mimeType;
    private String keyword;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAtStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAtEnd;
}
