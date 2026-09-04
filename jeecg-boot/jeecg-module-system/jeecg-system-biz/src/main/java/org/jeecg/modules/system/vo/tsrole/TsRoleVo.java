package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;
import org.jeecg.modules.system.vo.tscontenttag.TsContentTagDisplayVo;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceVo;

import java.util.Date;
import java.util.List;
import java.util.Map;
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

    /** 角色开场白。 */
    private String greeting;
    private String backgroundStory;
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
    private Integer contentVersion;
    private String reviewStatus;
    private Long currentReviewId;
    private Integer desiredPublic;
    private Date createdAt;
    private Date updatedAt;
    private List<TsContentTagDisplayVo> tags;
    private Map<String, TsImageResourceVo> imageResources;
}
