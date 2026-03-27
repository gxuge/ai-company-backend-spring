package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuservoiceconfig.TsUserVoiceConfigSaveDto;
import org.jeecg.modules.system.entity.TsUserVoiceConfig;
import org.jeecg.modules.system.vo.tsuservoiceconfig.TsUserVoiceConfigVo;
public interface ITsUserVoiceConfigService extends IService<TsUserVoiceConfig> {
    Result<TsUserVoiceConfigVo> getCurrentConfig(LoginUser user);
    Result<TsUserVoiceConfigVo> saveCurrentConfig(LoginUser user, TsUserVoiceConfigSaveDto request);
}
