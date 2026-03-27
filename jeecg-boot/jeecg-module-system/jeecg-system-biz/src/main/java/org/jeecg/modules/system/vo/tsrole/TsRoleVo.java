package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

import java.util.Date;
@Data
public class TsRoleVo {
    private Long id;
    private String userId;
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
    private Date createdAt;
    private Date updatedAt;
}