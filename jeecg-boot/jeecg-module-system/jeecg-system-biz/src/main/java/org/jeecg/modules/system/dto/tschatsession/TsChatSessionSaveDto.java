package org.jeecg.modules.system.dto.tschatsession;

import lombok.Data;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Data
public class TsChatSessionSaveDto {
    public interface Create {}
    public interface Update {}
    @NotNull(message = "编辑会话时id不能为空", groups = Update.class)
    private Long id;
    @NotBlank(message = "sessionType不能为空", groups = {Create.class, Update.class})
    private String sessionType;
    private String sessionTitle;
    private Long targetRoleId;
    private Long storyId;
    private Integer sessionStatus;
    private Long lastMessageId;
    private String extJson;
    public void applyCreateDefaults() {
        if (this.sessionStatus == null) {
            this.sessionStatus = 1;
        }
    }
    @AssertTrue(message = "sessionType仅支持single或story", groups = {Create.class, Update.class})
    public boolean isSessionTypeValid() {
        if (this.sessionType == null) {
            return false;
        }
        String value = this.sessionType.trim();
        return "single".equals(value) || "story".equals(value);
    }
    @AssertTrue(message = "single会话必须传targetRoleId", groups = {Create.class, Update.class})
    public boolean isSingleBindingValid() {
        if (this.sessionType == null) {
            return false;
        }
        String value = this.sessionType.trim();
        if (!"single".equals(value)) {
            return true;
        }
        return this.targetRoleId != null;
    }
    @AssertTrue(message = "story会话必须传storyId", groups = {Create.class, Update.class})
    public boolean isStoryBindingValid() {
        if (this.sessionType == null) {
            return false;
        }
        String value = this.sessionType.trim();
        if (!"story".equals(value)) {
            return true;
        }
        return this.storyId != null;
    }
}
