package org.jeecg.modules.system.dto.tsrole;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jeecg.modules.system.dto.tscontenttag.TsContentTagCandidateDto;

import java.util.List;
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
    /** 角色开场白。 */
    private String greeting;
    private String backgroundStory;
    private String dialoguePreview;
    private String dialogueLength;
    private String toneTendency;
    private String interactionMode;
    private String voiceName;
    private Long voiceProfileId;
    private String extJson;
    private Integer isPublic;
    private Integer basicAiGenerated;
    private Integer advancedAiGenerated;
    private Integer status;
    /** AI 生成阶段返回的候选内容标签。 */
    private List<TsContentTagCandidateDto> tags;
    /** 生成候选标签所使用的模型或提示词版本。 */
    private String tagModelVersion;

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
