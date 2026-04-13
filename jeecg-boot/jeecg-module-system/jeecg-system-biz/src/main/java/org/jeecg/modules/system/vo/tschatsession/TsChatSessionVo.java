package org.jeecg.modules.system.vo.tschatsession;

import lombok.Data;

import java.util.Date;
@Data
public class TsChatSessionVo {
    private Long id;
    private String userId;
    private String sessionType;
    /** 是否为内置系统会话（用于前端分流到系统聊天页） */
    private Boolean isSystemSession;
    private String sessionTitle;
    private Long targetRoleId;
    private Long storyId;
    private Integer sessionStatus;
    private Long lastMessageId;
    private Date lastMessageAt;
    private String extJson;
    private Date createdAt;
    private Date updatedAt;
}
