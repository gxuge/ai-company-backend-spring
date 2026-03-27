package org.jeecg.modules.system.vo.tschatmessageattachment;

import lombok.Data;

import java.util.Date;
@Data
public class TsChatMessageAttachmentVo {
    private Long id;
    private Long messageId;
    private String fileType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Integer durationSec;
    private String mimeType;
    private Date createdAt;
}