package org.jeecg.modules.system.vo.tsroletag;

import org.jeecg.modules.system.entity.TsRoleTag;

import java.util.ArrayList;
import java.util.List;

public final class TsRoleTagVoConverter {

    private TsRoleTagVoConverter() {
    }

    public static List<TsRoleTagVo> fromEntityList(List<TsRoleTag> source) {
        List<TsRoleTagVo> target = new ArrayList<>();
        if (source == null) {
            return target;
        }
        for (TsRoleTag item : source) {
            TsRoleTagVo vo = fromEntity(item);
            if (vo != null) {
                target.add(vo);
            }
        }
        return target;
    }

    public static TsRoleTagVo fromEntity(TsRoleTag entity) {
        if (entity == null) {
            return null;
        }
        TsRoleTagVo vo = new TsRoleTagVo();
        vo.setId(entity.getId());
        vo.setTagName(entity.getTagName());
        vo.setSortNo(entity.getSortNo());
        return vo;
    }
}

