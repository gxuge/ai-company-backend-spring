package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicActionDto;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicQueryDto;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicSaveDto;
import org.jeecg.modules.system.entity.TsRolePublic;
import org.jeecg.modules.system.vo.tsrolepublic.TsRolePublicTargetOptionVo;
import org.jeecg.modules.system.vo.tsrolepublic.TsRolePublicVo;

/**
 * 角色公开记录 Service。
 */
public interface ITsRolePublicService extends IService<TsRolePublic> {
    Result<Page<TsRolePublicVo>> pagePublics(LoginUser user, TsRolePublicQueryDto request);
    Result<TsRolePublicVo> getPublic(LoginUser user, Long id);
    Result<TsRolePublicVo> addPublic(LoginUser user, TsRolePublicSaveDto request);
    Result<TsRolePublicVo> editPublic(LoginUser user, Long id, TsRolePublicSaveDto request);
    Result<?> deletePublic(LoginUser user, Long id);
    Result<TsRolePublicVo> submitPublic(LoginUser user, TsRolePublicActionDto request);
    Result<TsRolePublicVo> approvePublic(LoginUser user, TsRolePublicActionDto request);
    Result<TsRolePublicVo> rejectPublic(LoginUser user, TsRolePublicActionDto request);
    Result<TsRolePublicVo> onlinePublic(LoginUser user, TsRolePublicActionDto request);
    Result<TsRolePublicVo> offlinePublic(LoginUser user, TsRolePublicActionDto request);
    Result<Page<TsRolePublicTargetOptionVo>> pageRoleOptions(LoginUser user, String ownerUserId, String keyword, Integer pageNo, Integer pageSize);
}
