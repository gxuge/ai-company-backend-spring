package org.jeecg.modules.system.dto.tsvoiceprofile;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
@Data
public class TsVoiceProfileTagSaveDto {
    @NotNull(message = "voiceProfileId不能为空")
    private Long voiceProfileId;
    private List<Long> tagIds;
    public void applyDefaults() {
        if (this.tagIds == null) {
            this.tagIds = new ArrayList<>();
        }
    }
    public List<Long> normalizeTagIds() {
        if (this.tagIds == null || this.tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> uniqueTagIds = new LinkedHashSet<>();
        for (Long tagId : this.tagIds) {
            if (tagId != null && tagId > 0) {
                uniqueTagIds.add(tagId);
            }
        }
        return new ArrayList<>(uniqueTagIds);
    }
}
