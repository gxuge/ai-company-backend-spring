package org.jeecg.modules.system.vo.tschatsession;

import lombok.Data;

import java.util.Date;

@Data
public class TsChatTemplateReplyVo {
    private Long sessionId;
    private Long activeRoleId;
    private Long userMessageId;
    private Long assistantMessageId;
    private String activeRoleName;
    private String contentText;
    private String promptCode;
    private String promptVersion;
    private String renderedPrompt;
    private String snapshotKey;
    private Date createdAt;
}
