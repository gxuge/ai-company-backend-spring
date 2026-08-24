package org.jeecg.modules.system.service;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickOutlineGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneImageGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStorySceneImagePromptOptimizeDto;
import org.jeecg.modules.system.vo.tsstory.TsStoryFullGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickOutlineGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneImageGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSettingGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStorySceneImagePromptOptimizeVo;

public interface ITsStoryGenerateService {
    TsStoryOneClickSettingGenerateVo generateStorySetting(LoginUser user, TsStoryOneClickSettingGenerateDto request);

    TsStoryOneClickSceneGenerateVo generateStoryScene(LoginUser user, TsStoryOneClickSceneGenerateDto request);

    TsStoryOneClickSceneImageGenerateVo generateStorySceneImage(LoginUser user, TsStoryOneClickSceneImageGenerateDto request);

    TsStorySceneImagePromptOptimizeVo optimizeStorySceneImagePrompt(LoginUser user, TsStorySceneImagePromptOptimizeDto request);

    TsStoryOneClickOutlineGenerateVo generateStoryOutline(LoginUser user, TsStoryOneClickOutlineGenerateDto request);

    TsStoryFullGenerateVo generateStoryFull(LoginUser user, TsStoryFullGenerateDto request);

    TsStoryFullGenerateVo generateStoryFullPreset(LoginUser user, TsStoryFullGenerateDto request);
}
