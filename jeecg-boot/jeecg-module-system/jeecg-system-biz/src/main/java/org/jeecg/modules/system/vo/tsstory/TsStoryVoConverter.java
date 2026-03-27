package org.jeecg.modules.system.vo.tsstory;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.entity.TsStoryRoleRel;
import org.jeecg.modules.system.entity.TsStoryStat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public final class TsStoryVoConverter {

    private TsStoryVoConverter() {
    }
    public static Page<TsStoryVo> fromPage(Page<TsStory> source,
                                            Map<Long, TsStoryStat> statMap,
                                            Map<Long, List<TsStoryRoleRel>> roleRelMap) {
        Page<TsStoryVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsStoryVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsStory story : source.getRecords()) {
                TsStoryStat stat = statMap == null ? null : statMap.get(story.getId());
                List<TsStoryRoleRel> relList = roleRelMap == null ? null : roleRelMap.get(story.getId());
                records.add(fromEntity(story, stat, relList));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsStoryVo fromEntity(TsStory story, TsStoryStat stat, List<TsStoryRoleRel> relList) {
        if (story == null) {
            return null;
        }
        TsStoryVo vo = new TsStoryVo();
        vo.setId(story.getId());
        vo.setStoryCode(story.getStoryCode());
        vo.setUserId(story.getUserId());
        vo.setTitle(story.getTitle());
        vo.setStoryIntro(story.getStoryIntro());
        vo.setStoryMode(story.getStoryMode());
        vo.setStorySetting(story.getStorySetting());
        vo.setStoryBackground(story.getStoryBackground());
        vo.setCoverUrl(story.getCoverUrl());
        vo.setSceneId(story.getSceneId());
        vo.setSceneNameSnapshot(story.getSceneNameSnapshot());
        vo.setStatus(story.getStatus());
        vo.setIsPublic(story.getIsPublic());
        vo.setIsAiStorySetting(story.getIsAiStorySetting());
        vo.setIsAiCharacter(story.getIsAiCharacter());
        vo.setIsAiOutline(story.getIsAiOutline());
        vo.setRemark(story.getRemark());
        vo.setCreatedBy(story.getCreatedBy());
        vo.setCreatedName(story.getCreatedName());
        vo.setUpdatedBy(story.getUpdatedBy());
        vo.setUpdatedName(story.getUpdatedName());
        vo.setCreatedAt(story.getCreatedAt());
        vo.setUpdatedAt(story.getUpdatedAt());
        vo.setIsDeleted(story.getIsDeleted());
        vo.setFollowerCount(stat == null ? 0L : safeLong(stat.getFollowerCount()));
        vo.setDialogueCount(stat == null ? 0L : safeLong(stat.getDialogueCount()));
        vo.setRoleBindings(toRoleBindings(relList));
        return vo;
    }
    public static List<TsStoryRoleBindingVo> toRoleBindings(List<TsStoryRoleRel> relList) {
        List<TsStoryRoleBindingVo> result = new ArrayList<>();
        if (relList == null || relList.isEmpty()) {
            return result;
        }
        for (TsStoryRoleRel rel : relList) {
            if (rel == null) {
                continue;
            }
            TsStoryRoleBindingVo item = new TsStoryRoleBindingVo();
            item.setRoleId(rel.getRoleId());
            item.setRoleType(rel.getRoleType());
            item.setSortNo(rel.getSortNo());
            item.setIsRequired(rel.getIsRequired());
            item.setJoinSource(rel.getJoinSource());
            result.add(item);
        }
        return result;
    }
    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}