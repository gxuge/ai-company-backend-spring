package org.jeecg.modules.system.vo.tsstorychapter;

import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class TsStoryChapterVo {
    private Long id;
    private Long storyId;
    private Integer chapterNo;
    private String chapterTitle;
    private String chapterDesc;
    private String openingContent;
    private Long openingRoleId;
    private String missionTarget;
    private Integer status;
    private Integer isAiGenerated;
    private Integer sortNo;
    private Date createdAt;
    private Date updatedAt;
    private List<Long> forbiddenRoleIds;
}