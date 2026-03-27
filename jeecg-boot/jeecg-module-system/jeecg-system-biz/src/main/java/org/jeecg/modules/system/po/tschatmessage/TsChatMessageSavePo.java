package org.jeecg.modules.system.po.tschatmessage;

import lombok.Data;
import org.jeecg.modules.system.dto.tschatmessage.TsChatMessageSaveDto;
import org.jeecg.modules.system.entity.TsChatMessage;
@Data
public class TsChatMessageSavePo {
    private Long sessionId;
    private String senderType;
    private Long senderId;
    private String senderName;
    private String messageType;
    private String contentText;
    private String contentJson;
    private Long replyToMessageId;
    private String generateStatus;
    public static TsChatMessageSavePo fromRequest(TsChatMessageSaveDto request) {
        TsChatMessageSavePo po = new TsChatMessageSavePo();
        if (request == null) {
            return po;
        }
        po.setSessionId(request.getSessionId());
        po.setSenderType(trimToNull(request.getSenderType()));
        po.setSenderId(request.getSenderId());
        po.setSenderName(trimToNull(request.getSenderName()));
        po.setMessageType(trimToNull(request.getMessageType()));
        po.setContentText(request.getContentText());
        po.setContentJson(request.getContentJson());
        po.setReplyToMessageId(request.getReplyToMessageId());
        po.setGenerateStatus(trimToNull(request.getGenerateStatus()));
        return po;
    }
    public void applyCreateTo(TsChatMessage entity) {
        if (entity == null) {
            return;
        }
        entity.setSessionId(this.sessionId);
        entity.setSenderType(this.senderType);
        entity.setSenderId(this.senderId);
        entity.setSenderName(this.senderName);
        entity.setMessageType(this.messageType);
        entity.setContentText(this.contentText);
        entity.setContentJson(this.contentJson);
        entity.setReplyToMessageId(this.replyToMessageId);
        entity.setGenerateStatus(this.generateStatus);
    }
    public void applyUpdateTo(TsChatMessage entity) {
        if (entity == null) {
            return;
        }
        if (this.senderType != null) {
            entity.setSenderType(this.senderType);
        }
        entity.setSenderId(this.senderId);
        entity.setSenderName(this.senderName);
        if (this.messageType != null) {
            entity.setMessageType(this.messageType);
        }
        entity.setContentText(this.contentText);
        entity.setContentJson(this.contentJson);
        entity.setReplyToMessageId(this.replyToMessageId);
        if (this.generateStatus != null) {
            entity.setGenerateStatus(this.generateStatus);
        }
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
