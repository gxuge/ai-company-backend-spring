package org.jeecg.modules.system.vo.tsuservoiceconfig;

import org.jeecg.modules.system.entity.TsUserVoiceConfig;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVo;

import java.math.BigDecimal;
public final class TsUserVoiceConfigVoConverter {

    private TsUserVoiceConfigVoConverter() {
    }
    public static TsUserVoiceConfigVo fromEntity(TsUserVoiceConfig entity, TsVoiceProfileVo selectedVoiceProfile) {
        if (entity == null) {
            return null;
        }
        TsUserVoiceConfigVo vo = new TsUserVoiceConfigVo();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setSelectedVoiceProfileId(entity.getSelectedVoiceProfileId());
        vo.setSelectedVoiceProfile(selectedVoiceProfile);
        vo.setPitchPercent(entity.getPitchPercent());
        vo.setSpeedRate(entity.getSpeedRate());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
    public static TsUserVoiceConfigVo fromDefault(String userId) {
        TsUserVoiceConfigVo vo = new TsUserVoiceConfigVo();
        vo.setUserId(userId);
        vo.setPitchPercent(BigDecimal.ZERO);
        vo.setSpeedRate(BigDecimal.ONE);
        return vo;
    }
    public static TsUserVoiceConfigVo fromEntity(TsUserVoiceConfig entity) {
        return fromEntity(entity, null);
    }
}
