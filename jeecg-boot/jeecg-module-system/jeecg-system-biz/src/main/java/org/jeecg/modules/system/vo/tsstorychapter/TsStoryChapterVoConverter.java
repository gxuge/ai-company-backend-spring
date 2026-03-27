package org.jeecg.modules.system.vo.tsstorychapter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsStoryChapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public final class TsStoryChapterVoConverter {

    private TsStoryChapterVoConverter() {
    }
    public static Page<TsStoryChapterVo> fromPage(Page<TsStoryChapter> source, Map<Long, List<Long>> forbiddenRoleMap) {
        Page<TsStoryChapterVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsStoryChapterVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsStoryChapter chapter : source.getRecords()) {
                List<Long> roleIds = forbiddenRoleMap == null ? null : forbiddenRoleMap.get(chapter.getId());
                records.add(fromEntity(chapter, roleIds));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsStoryChapterVo fromEntity(TsStoryChapter chapter, List<Long> forbiddenRoleIds) {
        if (chapter == null) {
            return null;
        }
        TsStoryChapterVo vo = new TsStoryChapterVo();
        vo.setId(chapter.getId());
        vo.setStoryId(chapter.getStoryId());
        vo.setChapterNo(chapter.getChapterNo());
        vo.setChapterTitle(chapter.getChapterTitle());
        vo.setChapterDesc(chapter.getChapterDesc());
        vo.setOpeningContent(chapter.getOpeningContent());
        vo.setOpeningRoleId(chapter.getOpeningRoleId());
        vo.setMissionTarget(chapter.getMissionTarget());
        vo.setStatus(chapter.getStatus());
        vo.setIsAiGenerated(chapter.getIsAiGenerated());
        vo.setSortNo(chapter.getSortNo());
        vo.setCreatedAt(chapter.getCreatedAt());
        vo.setUpdatedAt(chapter.getUpdatedAt());
        vo.setForbiddenRoleIds(forbiddenRoleIds == null ? new ArrayList<>() : forbiddenRoleIds);
        return vo;
    }
}