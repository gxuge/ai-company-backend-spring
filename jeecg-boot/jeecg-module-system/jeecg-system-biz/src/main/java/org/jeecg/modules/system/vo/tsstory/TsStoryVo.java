package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class TsStoryVo {
    private Long id;
    private String storyCode;
    private String userId;
    private String title;
    private String storyIntro;
    private String storyMode;
    private String storySetting;
    private String storyBackground;
    private String coverUrl;
    private Long sceneId;
    private String sceneNameSnapshot;
    private Integer status;
    private Integer isPublic;
    private Integer isAiStorySetting;
    private Integer isAiCharacter;
    private Integer isAiOutline;
    private String remark;
    private String createdBy;
    private String createdName;
    private String updatedBy;
    private String updatedName;
    private Date createdAt;
    private Date updatedAt;
    private Integer isDeleted;
    private Long followerCount;
    private Long dialogueCount;
    private List<TsStoryRoleBindingVo> roleBindings;
}