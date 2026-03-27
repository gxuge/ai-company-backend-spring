package org.jeecg.modules.system.vo.tsvoiceprofile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public final class TsVoiceProfileVoConverter {

    private TsVoiceProfileVoConverter() {
    }
    public static Page<TsVoiceProfileVo> fromPage(Page<TsVoiceProfile> source,
                                                   Map<Long, List<Long>> profileTagIds,
                                                   Map<Long, TsVoiceTagVo> tagVoMap) {
        Page<TsVoiceProfileVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsVoiceProfileVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsVoiceProfile profile : source.getRecords()) {
                List<Long> tagIds = profileTagIds == null ? null : profileTagIds.get(profile.getId());
                List<TsVoiceTagVo> tags = new ArrayList<>();
                if (tagIds != null && tagVoMap != null) {
                    for (Long tagId : tagIds) {
                        TsVoiceTagVo tagVo = tagVoMap.get(tagId);
                        if (tagVo != null) {
                            tags.add(tagVo);
                        }
                    }
                }
                records.add(fromEntity(profile, tagIds, tags));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsVoiceProfileVo fromEntity(TsVoiceProfile entity, List<Long> tagIds, List<TsVoiceTagVo> tags) {
        if (entity == null) {
            return null;
        }
        TsVoiceProfileVo vo = new TsVoiceProfileVo();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setAvatarUrl(entity.getAvatarUrl());
        vo.setGender(entity.getGender());
        vo.setAgeGroup(entity.getAgeGroup());
        vo.setStatus(entity.getStatus());
        vo.setSortNo(entity.getSortNo());
        vo.setTagIds(tagIds == null ? new ArrayList<>() : new ArrayList<>(tagIds));
        vo.setTags(tags == null ? new ArrayList<>() : new ArrayList<>(tags));
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
    public static TsVoiceProfileVo fromEntity(TsVoiceProfile entity) {
        return fromEntity(entity, null, null);
    }
}
