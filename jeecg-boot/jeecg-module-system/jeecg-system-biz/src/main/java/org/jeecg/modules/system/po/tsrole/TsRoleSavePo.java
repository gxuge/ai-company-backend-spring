package org.jeecg.modules.system.po.tsrole;

import lombok.Data;
import org.jeecg.modules.system.dto.tsrole.TsRoleSaveDto;
import org.jeecg.modules.system.entity.TsRole;
@Data
public class TsRoleSavePo {
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
    public static TsRoleSavePo fromRequest(TsRoleSaveDto request) {
        TsRoleSavePo po = new TsRoleSavePo();
        if (request == null) {
            return po;
        }
        po.setRoleName(trimToNull(request.getRoleName()));
        po.setRoleSubtitle(trimToNull(request.getRoleSubtitle()));
        po.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        po.setCoverUrl(trimToNull(request.getCoverUrl()));
        po.setGender(trimToNull(request.getGender()));
        po.setOccupation(trimToNull(request.getOccupation()));
        po.setIntroText(request.getIntroText());
        po.setPersonaText(request.getPersonaText());
        po.setBackgroundStory(request.getBackgroundStory());
        po.setStoryText(request.getStoryText());
        po.setDialoguePreview(request.getDialoguePreview());
        po.setDialogueLength(trimToNull(request.getDialogueLength()));
        po.setToneTendency(trimToNull(request.getToneTendency()));
        po.setInteractionMode(trimToNull(request.getInteractionMode()));
        po.setVoiceName(trimToNull(request.getVoiceName()));
        po.setExtJson(request.getExtJson());
        po.setIsPublic(request.getIsPublic());
        po.setBasicAiGenerated(request.getBasicAiGenerated());
        po.setAdvancedAiGenerated(request.getAdvancedAiGenerated());
        po.setStatus(request.getStatus());
        return po;
    }
    public void applyTo(TsRole role) {
        if (role == null) {
            return;
        }
        role.setRoleName(this.roleName);
        role.setRoleSubtitle(this.roleSubtitle);
        role.setAvatarUrl(this.avatarUrl);
        role.setCoverUrl(this.coverUrl);
        role.setGender(this.gender);
        role.setOccupation(this.occupation);
        role.setIntroText(this.introText);
        role.setPersonaText(this.personaText);
        role.setBackgroundStory(this.backgroundStory);
        role.setStoryText(this.storyText);
        role.setDialoguePreview(this.dialoguePreview);
        role.setDialogueLength(this.dialogueLength);
        role.setToneTendency(this.toneTendency);
        role.setInteractionMode(this.interactionMode);
        role.setVoiceName(this.voiceName);
        role.setExtJson(this.extJson);
        role.setIsPublic(this.isPublic);
        role.setBasicAiGenerated(this.basicAiGenerated);
        role.setAdvancedAiGenerated(this.advancedAiGenerated);
        role.setStatus(this.status);
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}