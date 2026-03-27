package org.jeecg.modules.system.dto.tsuservoiceconfig;

import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
@Data
public class TsUserVoiceConfigSaveDto {
    @NotNull(message = "selectedVoiceProfileId不能为空")
    private Long selectedVoiceProfileId;
    @DecimalMin(value = "-100.00", message = "pitchPercent不能小于-100")
    @DecimalMax(value = "100.00", message = "pitchPercent不能大于100")
    private BigDecimal pitchPercent;
    @DecimalMin(value = "0.50", message = "speedRate不能小于0.5")
    @DecimalMax(value = "3.00", message = "speedRate不能大于3.0")
    private BigDecimal speedRate;
    public void applyDefaults() {
        if (this.pitchPercent == null) {
            this.pitchPercent = BigDecimal.ZERO;
        }
        if (this.speedRate == null) {
            this.speedRate = BigDecimal.ONE;
        }
    }
}
