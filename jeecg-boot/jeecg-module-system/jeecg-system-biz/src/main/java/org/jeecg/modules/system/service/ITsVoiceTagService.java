package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsvoicetag.TsVoiceTagQueryDto;
import org.jeecg.modules.system.dto.tsvoicetag.TsVoiceTagSaveDto;
import org.jeecg.modules.system.entity.TsVoiceTag;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;
public interface ITsVoiceTagService extends IService<TsVoiceTag> {
    Result<Page<TsVoiceTagVo>> pageVoiceTags(LoginUser user, TsVoiceTagQueryDto request);
    Result<TsVoiceTagVo> addVoiceTag(LoginUser user, TsVoiceTagSaveDto request);
    Result<?> deleteVoiceTag(LoginUser user, Long id);
}
