package org.jeecg.modules.system.vo.tsroleimageprofile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsRoleImageProfile;

import java.util.ArrayList;
import java.util.List;
public final class TsRoleImageProfileVoConverter {

    private TsRoleImageProfileVoConverter() {
    }
    public static Page<TsRoleImageProfileVo> fromPage(Page<TsRoleImageProfile> source) {
        Page<TsRoleImageProfileVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsRoleImageProfileVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsRoleImageProfile item : source.getRecords()) {
                records.add(fromEntity(item));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsRoleImageProfileVo fromEntity(TsRoleImageProfile entity) {
        if (entity == null) {
            return null;
        }
        TsRoleImageProfileVo vo = new TsRoleImageProfileVo();
        vo.setId(entity.getId());
        vo.setProfileName(entity.getProfileName());
        vo.setOwnerUserId(entity.getOwnerUserId());
        vo.setPromptText(entity.getPromptText());
        vo.setStyleName(entity.getStyleName());
        vo.setSelectedImageAssetId(entity.getSelectedImageAssetId());
        vo.setSelectedImageUrl(entity.getSelectedImageUrl());
        vo.setSourceType(entity.getSourceType());
        vo.setIsPublic(entity.getIsPublic());
        vo.setStatus(entity.getStatus());
        vo.setExtJson(entity.getExtJson());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}