package org.jeecg.modules.system.vo.tschatsession;

import lombok.Data;

import java.util.Date;
@Data
public class TsChatSessionVo {
    private Long id;
    private String userId;
    private String sessionType;
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
