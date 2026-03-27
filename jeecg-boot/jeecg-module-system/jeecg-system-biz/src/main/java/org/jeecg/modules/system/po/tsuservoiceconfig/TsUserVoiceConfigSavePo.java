package org.jeecg.modules.system.po.tsuservoiceconfig;

import lombok.Data;
import org.jeecg.modules.system.dto.tsuservoiceconfig.TsUserVoiceConfigSaveDto;
import org.jeecg.modules.system.entity.TsUserVoiceConfig;

import java.math.BigDecimal;
@Data
public class TsUserVoiceConfigSavePo {
    private Long selectedVoiceProfileId;
    private BigDecimal pitchPercent;
    private BigDecimal speedRate;
    public static TsUserVoiceConfigSavePo fromRequest(TsUserVoiceConfigSaveDto request) {
        TsUserVoiceConfigSavePo po = new TsUserVoiceConfigSavePo();
        if (request == null) {
            return po;
        }
        po.setSelectedVoiceProfileId(request.getSelectedVoiceProfileId());
        po.setPitchPercent(request.getPitchPercent());
        po.setSpeedRate(request.getSpeedRate());
        return po;
    }
    public void applyTo(TsUserVoiceConfig entity) {
        if (entity == null) {
            return;
        }
        entity.setSelectedVoiceProfileId(this.selectedVoiceProfileId);
        entity.setPitchPercent(this.pitchPercent);
        entity.setSpeedRate(this.speedRate);
    }
}
