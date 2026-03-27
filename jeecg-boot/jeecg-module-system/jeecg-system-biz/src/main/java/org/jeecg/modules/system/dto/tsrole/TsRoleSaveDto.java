package org.jeecg.modules.system.dto.tsrole;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Data
public class TsRoleSaveDto {
    public interface Create {}
    public interface Update {}
    @NotNull(message = "编辑角色时id不能为空", groups = Update.class)
    private Long id;
    @NotBlank(message = "角色名称不能为空", groups = {Create.class, Update.class})
    private String roleName;
    private String roleSubtitle;
    private String avatarUrl;
    private String coverUrl;
    private String gender;
    private String occupation;
    private String introText;
    private String personaText;
    private String backgroundStory;
    private String storyText;
    private String dialoguePreview;
    private String dialogueLength;
    private String toneTendency;
    private String interactionMode;
    private String voiceName;
    private String extJson;
    private Integer isPublic;
    private Integer basicAiGenerated;
    private Integer advancedAiGenerated;
    private Integer status;
    public void applyCreateDefaults() {
        if (this.status == null) {
            this.status = 1;
        }
        if (this.isPublic == null) {
            this.isPublic = 0;
        }
        if (this.basicAiGenerated == null) {
            this.basicAiGenerated = 0;
        }
        if (this.advancedAiGenerated == null) {
            this.advancedAiGenerated = 0;
        }
    }
}