package org.jeecg.modules.system.service;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickOutlineGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSettingGenerateDto;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickOutlineGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSettingGenerateVo;

public interface ITsStoryGenerateService {
    TsStoryOneClickSettingGenerateVo generateStorySetting(LoginUser user, TsStoryOneClickSettingGenerateDto request);

    TsStoryOneClickSceneGenerateVo generateStoryScene(LoginUser user, TsStoryOneClickSceneGenerateDto request);

    TsStoryOneClickOutlineGenerateVo generateStoryOutline(LoginUser user, TsStoryOneClickOutlineGenerateDto request);
}
