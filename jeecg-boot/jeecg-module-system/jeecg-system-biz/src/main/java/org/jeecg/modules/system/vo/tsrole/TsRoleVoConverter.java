package org.jeecg.modules.system.vo.tsrole;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsRole;

import java.util.ArrayList;
import java.util.List;
public final class TsRoleVoConverter {

    private TsRoleVoConverter() {
    }
    public static Page<TsRoleVo> fromPage(Page<TsRole> source) {
        Page<TsRoleVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsRoleVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsRole role : source.getRecords()) {
                records.add(fromEntity(role));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsRoleVo fromEntity(TsRole role) {
        if (role == null) {
            return null;
        }
        TsRoleVo vo = new TsRoleVo();
        vo.setId(role.getId());
        vo.setUserId(role.getUserId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleSubtitle(role.getRoleSubtitle());
        vo.setAvatarUrl(role.getAvatarUrl());
        vo.setCoverUrl(role.getCoverUrl());
        vo.setGender(role.getGender());
        vo.setOccupation(role.getOccupation());
        vo.setIntroText(role.getIntroText());
        vo.setPersonaText(role.getPersonaText());
        vo.setBackgroundStory(role.getBackgroundStory());
        vo.setStoryText(role.getStoryText());
        vo.setDialoguePreview(role.getDialoguePreview());
        vo.setDialogueLength(role.getDialogueLength());
        vo.setToneTendency(role.getToneTendency());
        vo.setInteractionMode(role.getInteractionMode());
        vo.setVoiceName(role.getVoiceName());
        vo.setExtJson(role.getExtJson());
        vo.setIsPublic(role.getIsPublic());
        vo.setBasicAiGenerated(role.getBasicAiGenerated());
        vo.setAdvancedAiGenerated(role.getAdvancedAiGenerated());
        vo.setStatus(role.getStatus());
        vo.setCreatedAt(role.getCreatedAt());
        vo.setUpdatedAt(role.getUpdatedAt());
        return vo;
    }
}