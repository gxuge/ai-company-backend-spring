package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

import java.util.List;

@Data
public class TsStoryOneClickOutlineChapterVo {
    private Integer chapterNo;
    private String chapterTitle;
    private String chapterDesc;
    private String openingContent;
    private String openingRoleName;
    private String missionTarget;
    private List<String> forbiddenRoleNames;
}
