package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceVo;

import java.util.Date;
import java.util.List;
import java.util.Map;
@Data
public class TsStoryVo {
    private Long id;
    private String storyCode;
    private String userId;
    private String title;
    private String storyIntro;
    private String storyMode;
    private String storySetting;
    private String siteSetting;
    private String storyBackground;
    private String coverUrl;
    private String sceneImageUrl;
    private Long sceneId;
    private String sceneNameSnapshot;
    private Integer status;
    private Integer isPublic;
    private Integer isAiStorySetting;
    private Integer isAiCharacter;
    private Integer isAiOutline;
    private String plotOutline;
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
    private Map<String, TsImageResourceVo> imageResources;
}
