package org.jeecg.modules.system.po.tschatsession;

import lombok.Data;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionSaveDto;
import org.jeecg.modules.system.entity.TsChatSession;
@Data
public class TsChatSessionSavePo {
    private String sessionType;
    private String sessionTitle;
    private Long targetRoleId;
    private Long storyId;
    private Integer sessionStatus;
    private Long lastMessageId;
    private String extJson;
    public static TsChatSessionSavePo fromRequest(TsChatSessionSaveDto request) {
        TsChatSessionSavePo po = new TsChatSessionSavePo();
        if (request == null) {
            return po;
        }
        po.setSessionType(trimToNull(request.getSessionType()));
        po.setSessionTitle(trimToNull(request.getSessionTitle()));
        po.setTargetRoleId(request.getTargetRoleId());
        po.setStoryId(request.getStoryId());
        po.setSessionStatus(request.getSessionStatus());
        po.setLastMessageId(request.getLastMessageId());
        po.setExtJson(request.getExtJson());
        return po;
    }
    public void applyCreateTo(TsChatSession entity) {
        if (entity == null) {
            return;
        }
        entity.setSessionType(this.sessionType);
        entity.setSessionTitle(this.sessionTitle);
        entity.setTargetRoleId(this.targetRoleId);
        entity.setStoryId(this.storyId);
        entity.setSessionStatus(this.sessionStatus);
        entity.setLastMessageId(this.lastMessageId);
        entity.setExtJson(this.extJson);
    }
    public void applyUpdateTo(TsChatSession entity) {
        if (entity == null) {
            return;
        }
        if (this.sessionType != null) {
            entity.setSessionType(this.sessionType);
        }
        entity.setSessionTitle(this.sessionTitle);
        entity.setTargetRoleId(this.targetRoleId);
        entity.setStoryId(this.storyId);
        if (this.sessionStatus != null) {
            entity.setSessionStatus(this.sessionStatus);
        }
        entity.setLastMessageId(this.lastMessageId);
        entity.setExtJson(this.extJson);
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
