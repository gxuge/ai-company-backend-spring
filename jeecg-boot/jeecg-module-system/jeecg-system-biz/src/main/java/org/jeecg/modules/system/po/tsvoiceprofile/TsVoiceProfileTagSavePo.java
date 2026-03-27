package org.jeecg.modules.system.po.tsvoiceprofile;

import lombok.Data;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileTagSaveDto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
@Data
public class TsVoiceProfileTagSavePo {
    private Long voiceProfileId;
    private List<Long> tagIds;
    public static TsVoiceProfileTagSavePo fromRequest(TsVoiceProfileTagSaveDto request) {
        TsVoiceProfileTagSavePo po = new TsVoiceProfileTagSavePo();
        if (request == null) {
            po.setTagIds(new ArrayList<>());
            return po;
        }
        po.setVoiceProfileId(request.getVoiceProfileId());
        po.setTagIds(normalizeTagIds(request.getTagIds()));
        return po;
    }
    private static List<Long> normalizeTagIds(List<Long> source) {
        List<Long> normalized = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return normalized;
        }
        Set<Long> uniq = new LinkedHashSet<>();
        for (Long item : source) {
            if (item != null && item > 0L) {
                uniq.add(item);
            }
        }
        normalized.addAll(uniq);
        return normalized;
    }
}