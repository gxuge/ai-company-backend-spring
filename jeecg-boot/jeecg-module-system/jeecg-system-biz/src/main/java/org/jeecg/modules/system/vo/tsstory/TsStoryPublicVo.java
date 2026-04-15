package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

import java.util.Date;

@Data
public class TsStoryPublicVo {
    private Long id;
    private String title;
    private String storyIntro;
    private String storyMode;
    private String coverUrl;
    private Long followerCount;
    private Long dialogueCount;
    private String authorName;
    private String authorAvatar;
    private Date updatedAt;
}

