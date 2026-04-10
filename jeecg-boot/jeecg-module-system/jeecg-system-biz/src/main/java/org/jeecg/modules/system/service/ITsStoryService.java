package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickOutlineGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryQueryDto;
import org.jeecg.modules.system.dto.tsstory.TsStorySaveDto;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickOutlineGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSettingGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryVo;
public interface ITsStoryService extends IService<TsStory> {
    Result<Page<TsStoryVo>> pageStories(LoginUser user, TsStoryQueryDto request);
    Result<TsStoryVo> getStory(LoginUser user, Long id);
    Result<TsStoryVo> addStory(LoginUser user, TsStorySaveDto request);
    Result<TsStoryVo> editStory(LoginUser user, Long id, TsStorySaveDto request);
    Result<?> deleteStory(LoginUser user, Long id);
    Result<TsStoryOneClickSettingGenerateVo> generateStorySetting(LoginUser user, TsStoryOneClickSettingGenerateDto request);
    Result<TsStoryOneClickSceneGenerateVo> generateStoryScene(LoginUser user, TsStoryOneClickSceneGenerateDto request);
    Result<TsStoryOneClickOutlineGenerateVo> generateStoryOutline(LoginUser user, TsStoryOneClickOutlineGenerateDto request);
}
