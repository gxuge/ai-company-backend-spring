package org.jeecg.modules.system.vo.tsroleimagegeneraterecord;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsRoleImageGenerateRecord;

import java.util.ArrayList;
import java.util.List;
public final class TsRoleImageGenerateRecordVoConverter {

    private TsRoleImageGenerateRecordVoConverter() {
    }
    public static Page<TsRoleImageGenerateRecordVo> fromPage(Page<TsRoleImageGenerateRecord> source) {
        Page<TsRoleImageGenerateRecordVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsRoleImageGenerateRecordVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsRoleImageGenerateRecord item : source.getRecords()) {
                records.add(fromEntity(item));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsRoleImageGenerateRecordVo fromEntity(TsRoleImageGenerateRecord entity) {
        if (entity == null) {
            return null;
        }
        TsRoleImageGenerateRecordVo vo = new TsRoleImageGenerateRecordVo();
        vo.setId(entity.getId());
        vo.setRoleId(entity.getRoleId());
        vo.setSourceProfileUrl(entity.getSourceProfileUrl());
        vo.setUserId(entity.getUserId());
        vo.setPromptText(entity.getPromptText());
        vo.setStyleName(entity.getStyleName());
        vo.setReferenceAssetsJson(entity.getReferenceAssetsJson());
        vo.setGenerateStatus(entity.getGenerateStatus());
        vo.setApplyStatus(entity.getApplyStatus());
        vo.setResultAssetId(entity.getResultAssetId());
        vo.setResultImageUrl(entity.getResultImageUrl());
        vo.setFailReason(entity.getFailReason());
        vo.setRequestId(entity.getRequestId());
        vo.setExtJson(entity.getExtJson());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}