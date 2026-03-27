package org.jeecg.modules.system.vo.tsuservoiceconfig;

import lombok.Data;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVo;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class TsUserVoiceConfigVo {
    private Long id;
    private String userId;
    private Long selectedVoiceProfileId;
    private TsVoiceProfileVo selectedVoiceProfile;
    private BigDecimal pitchPercent;
    private BigDecimal speedRate;
    private Date createdAt;
    private Date updatedAt;
}
