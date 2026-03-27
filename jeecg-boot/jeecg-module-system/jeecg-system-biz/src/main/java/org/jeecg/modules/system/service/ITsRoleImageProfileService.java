package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileQueryDto;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileSaveDto;
import org.jeecg.modules.system.entity.TsRoleImageProfile;
import org.jeecg.modules.system.vo.tsroleimageprofile.TsRoleImageProfileVo;
public interface ITsRoleImageProfileService extends IService<TsRoleImageProfile> {
    Result<Page<TsRoleImageProfileVo>> pageProfiles(LoginUser user, TsRoleImageProfileQueryDto request);
    Result<TsRoleImageProfileVo> getProfile(LoginUser user, Long id);
    Result<TsRoleImageProfileVo> addProfile(LoginUser user, TsRoleImageProfileSaveDto request);
    Result<TsRoleImageProfileVo> editProfile(LoginUser user, Long id, TsRoleImageProfileSaveDto request);
    Result<?> deleteProfile(LoginUser user, Long id);
}