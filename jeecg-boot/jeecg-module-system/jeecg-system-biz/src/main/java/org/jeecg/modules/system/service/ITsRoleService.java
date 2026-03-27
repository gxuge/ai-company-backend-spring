package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsrole.TsRoleQueryDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleSaveDto;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.vo.tsrole.TsRoleVo;
public interface ITsRoleService extends IService<TsRole> {
    Result<Page<TsRoleVo>> pageRoles(LoginUser user, TsRoleQueryDto request);
    Result<TsRoleVo> getRole(LoginUser user, Long id);
    Result<TsRoleVo> addRole(LoginUser user, TsRoleSaveDto request);
    Result<TsRoleVo> editRole(LoginUser user, Long id, TsRoleSaveDto request);
    Result<?> deleteRole(LoginUser user, Long id);
}