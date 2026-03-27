package org.jeecg.modules.system.dto.tschatmessage;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Data
public class TsChatMessageSaveDto {
    public interface Create {}
    public interface Update {}
    @NotNull(message = "编辑消息时id不能为空", groups = Update.class)
    private Long id;
    @NotNull(message = "sessionId不能为空", groups = Create.class)
    private Long sessionId;
    @NotBlank(message = "senderType不能为空", groups = Create.class)
    private String senderType;
    private Long senderId;
    private String senderName;
    private String messageType;
    private String contentText;
    private String contentJson;
    private Long replyToMessageId;
    private String generateStatus;
    public void applyCreateDefaults() {
        if (this.messageType == null || this.messageType.trim().isEmpty()) {
            this.messageType = "text";
        }
        if (this.generateStatus == null || this.generateStatus.trim().isEmpty()) {
            this.generateStatus = "success";
        }
    }
}
