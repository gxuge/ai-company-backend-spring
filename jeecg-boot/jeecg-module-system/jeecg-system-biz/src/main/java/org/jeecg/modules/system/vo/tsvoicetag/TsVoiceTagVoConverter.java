package org.jeecg.modules.system.vo.tsvoicetag;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsVoiceTag;

import java.util.ArrayList;
import java.util.List;
public final class TsVoiceTagVoConverter {

    private TsVoiceTagVoConverter() {
    }
    public static Page<TsVoiceTagVo> fromPage(Page<TsVoiceTag> source) {
        Page<TsVoiceTagVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsVoiceTagVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsVoiceTag tag : source.getRecords()) {
                records.add(fromEntity(tag));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsVoiceTagVo fromEntity(TsVoiceTag entity) {
        if (entity == null) {
            return null;
        }
        TsVoiceTagVo vo = new TsVoiceTagVo();
        vo.setId(entity.getId());
        vo.setTagName(entity.getTagName());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
