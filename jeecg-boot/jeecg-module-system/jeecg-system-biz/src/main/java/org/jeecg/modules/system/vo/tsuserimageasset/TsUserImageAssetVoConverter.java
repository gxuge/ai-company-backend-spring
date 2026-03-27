package org.jeecg.modules.system.vo.tsuserimageasset;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsUserImageAsset;

import java.util.ArrayList;
import java.util.List;
public final class TsUserImageAssetVoConverter {

    private TsUserImageAssetVoConverter() {
    }
    public static Page<TsUserImageAssetVo> fromPage(Page<TsUserImageAsset> source) {
        Page<TsUserImageAssetVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsUserImageAssetVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsUserImageAsset item : source.getRecords()) {
                records.add(fromEntity(item));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsUserImageAssetVo fromEntity(TsUserImageAsset entity) {
        if (entity == null) {
            return null;
        }
        TsUserImageAssetVo vo = new TsUserImageAssetVo();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setFileUrl(entity.getFileUrl());
        vo.setThumbnailUrl(entity.getThumbnailUrl());
        vo.setFileName(entity.getFileName());
        vo.setMimeType(entity.getMimeType());
        vo.setFileSize(entity.getFileSize());
        vo.setWidth(entity.getWidth());
        vo.setHeight(entity.getHeight());
        vo.setSourceType(entity.getSourceType());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
