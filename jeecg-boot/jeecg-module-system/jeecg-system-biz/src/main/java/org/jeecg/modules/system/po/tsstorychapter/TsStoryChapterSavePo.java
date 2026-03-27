package org.jeecg.modules.system.po.tsstorychapter;

import lombok.Data;
import org.jeecg.modules.system.dto.tsstorychapter.TsStoryChapterSaveDto;
import org.jeecg.modules.system.entity.TsStoryChapter;

import java.util.Date;
@Data
public class TsStoryChapterSavePo {
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
    public static TsStoryChapterSavePo fromRequest(TsStoryChapterSaveDto request) {
        TsStoryChapterSavePo po = new TsStoryChapterSavePo();
        if (request == null) {
            return po;
        }
        po.setStoryId(request.getStoryId());
        po.setChapterNo(request.getChapterNo());
        po.setChapterTitle(trimToNull(request.getChapterTitle()));
        po.setChapterDesc(trimToNull(request.getChapterDesc()));
        po.setOpeningContent(trimToNull(request.getOpeningContent()));
        po.setOpeningRoleId(request.getOpeningRoleId());
        po.setMissionTarget(trimToNull(request.getMissionTarget()));
        po.setStatus(request.getStatus());
        po.setIsAiGenerated(request.getIsAiGenerated());
        po.setSortNo(request.getSortNo());
        return po;
    }
    public void applyTo(TsStoryChapter chapter) {
        if (chapter == null) {
            return;
        }
        chapter.setStoryId(this.storyId);
        chapter.setChapterNo(this.chapterNo);
        chapter.setChapterTitle(this.chapterTitle);
        chapter.setChapterDesc(this.chapterDesc);
        chapter.setOpeningContent(this.openingContent);
        chapter.setOpeningRoleId(this.openingRoleId);
        chapter.setMissionTarget(this.missionTarget);
        chapter.setStatus(this.status);
        chapter.setIsAiGenerated(this.isAiGenerated);
        chapter.setSortNo(this.sortNo);
    }
    public void applyForCreate(TsStoryChapter chapter, Date now) {
        applyTo(chapter);
        chapter.setCreatedAt(now);
        chapter.setUpdatedAt(now);
    }
    public void applyForUpdate(TsStoryChapter chapter, Date now) {
        applyTo(chapter);
        chapter.setUpdatedAt(now);
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}