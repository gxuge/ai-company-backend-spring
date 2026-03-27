package org.jeecg.modules.system.po.tsvoicetag;

import lombok.Data;
import org.jeecg.modules.system.dto.tsvoicetag.TsVoiceTagSaveDto;
import org.jeecg.modules.system.entity.TsVoiceTag;
@Data
public class TsVoiceTagSavePo {
    private String tagName;
    public static TsVoiceTagSavePo fromRequest(TsVoiceTagSaveDto request) {
        TsVoiceTagSavePo po = new TsVoiceTagSavePo();
        if (request == null) {
            return po;
        }
        po.setTagName(trimToNull(request.getTagName()));
        return po;
    }
    public void applyTo(TsVoiceTag entity) {
        if (entity == null) {
            return;
        }
        entity.setTagName(this.tagName);
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
