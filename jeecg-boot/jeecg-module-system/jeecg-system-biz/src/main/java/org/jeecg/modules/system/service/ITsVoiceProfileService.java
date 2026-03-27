package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileQueryDto;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileTagSaveDto;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVo;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;

import java.util.List;
public interface ITsVoiceProfileService extends IService<TsVoiceProfile> {
    Result<Page<TsVoiceProfileVo>> pageVoiceProfiles(LoginUser user, TsVoiceProfileQueryDto request);
    Result<?> deleteVoiceProfile(LoginUser user, Long id);
    Result<List<TsVoiceTagVo>> getVoiceProfileTags(LoginUser user, Long id);
    Result<List<TsVoiceTagVo>> saveVoiceProfileTags(LoginUser user, TsVoiceProfileTagSaveDto request);
}
