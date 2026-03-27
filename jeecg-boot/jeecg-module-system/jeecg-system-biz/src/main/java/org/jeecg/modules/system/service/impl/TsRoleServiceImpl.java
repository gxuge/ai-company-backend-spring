package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsRoleOwnershipAspect;
import org.jeecg.modules.aop.TsRoleOwnershipAspect.CheckTsRoleOwnership;
import org.jeecg.modules.system.dto.tsrole.TsRoleQueryDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleSaveDto;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.po.tsrole.TsRoleQueryPo;
import org.jeecg.modules.system.po.tsrole.TsRoleSavePo;
import org.jeecg.modules.system.service.ITsRoleService;
import org.jeecg.modules.system.vo.tsrole.TsRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Service
public class TsRoleServiceImpl extends ServiceImpl<TsRoleMapper, TsRole> implements ITsRoleService {
    @Override
    public Result<Page<TsRoleVo>> pageRoles(LoginUser user, TsRoleQueryDto request) {
        String userId = user.getId();
        TsRoleQueryPo queryPo = TsRoleQueryPo.fromRequest(userId, request);
        Page<TsRole> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsRole> pageData = baseMapper.selectRolePage(page, queryPo);
        return Result.OK(TsRoleVoConverter.fromPage(pageData));
    }
    @Override
    @CheckTsRoleOwnership(message = "角色不存在或无权限访问")
    public Result<TsRoleVo> getRole(LoginUser user, Long id) {
        TsRole role = TsRoleOwnershipAspect.ROLE_CONTEXT.get();
        return Result.OK(TsRoleVoConverter.fromEntity(role));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRoleVo> addRole(LoginUser user, TsRoleSaveDto request) {
        String userId = user.getId();
        request.applyCreateDefaults();
        TsRoleSavePo savePo = TsRoleSavePo.fromRequest(request);

        TsRole role = new TsRole();
        savePo.applyTo(role);
        role.setUserId(userId);
        role.setCreatedAt(new Date());
        role.setUpdatedAt(new Date());
        this.save(role);

        return Result.OK("新增成功", TsRoleVoConverter.fromEntity(role));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsRoleOwnership(message = "角色不存在或无权限访问")
    public Result<TsRoleVo> editRole(LoginUser user, Long id, TsRoleSaveDto request) {
        TsRole role = TsRoleOwnershipAspect.ROLE_CONTEXT.get();

        TsRoleSavePo savePo = TsRoleSavePo.fromRequest(request);
        savePo.applyTo(role);
        role.setUpdatedAt(new Date());
        this.updateById(role);

        return Result.OK("修改成功", TsRoleVoConverter.fromEntity(role));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsRoleOwnership(message = "角色不存在或无权限访问")
    public Result<?> deleteRole(LoginUser user, Long id) {
        TsRole role = TsRoleOwnershipAspect.ROLE_CONTEXT.get();
        role.setStatus(0);
        role.setUpdatedAt(new Date());
        this.updateById(role);
        return Result.OK("删除成功");
    }
}