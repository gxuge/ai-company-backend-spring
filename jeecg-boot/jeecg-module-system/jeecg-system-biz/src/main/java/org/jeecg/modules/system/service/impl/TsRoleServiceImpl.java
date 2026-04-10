package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsRoleOwnershipAspect;
import org.jeecg.modules.aop.TsRoleOwnershipAspect.CheckTsRoleOwnership;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateRoleDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickVoiceGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleQueryDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleSaveDto;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.po.tsrole.TsRoleQueryPo;
import org.jeecg.modules.system.po.tsrole.TsRoleSavePo;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.service.ITsRoleService;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickSettingGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickVoiceGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class TsRoleServiceImpl extends ServiceImpl<TsRoleMapper, TsRole> implements ITsRoleService {
    @Resource
    private ITsRoleGenerateService tsRoleGenerateService;

    /**
     * 分页查询当前用户角色列表。
     */
    @Override
    public Result<Page<TsRoleVo>> pageRoles(LoginUser user, TsRoleQueryDto request) {
        String userId = user.getId();
        TsRoleQueryPo queryPo = TsRoleQueryPo.fromRequest(userId, request);
        Page<TsRole> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsRole> pageData = baseMapper.selectRolePage(page, queryPo);
        return Result.OK(TsRoleVoConverter.fromPage(pageData));
    }

    /**
     * 查询角色详情，归属校验由 AOP 完成。
     */
    @Override
    @CheckTsRoleOwnership(message = "角色不存在或无权访问")
    public Result<TsRoleVo> getRole(LoginUser user, Long id) {
        TsRole role = TsRoleOwnershipAspect.ROLE_CONTEXT.get();
        return Result.OK(TsRoleVoConverter.fromEntity(role));
    }

    /**
     * 新增角色并写入创建默认值。
     */
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

    /**
     * 编辑角色基础信息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsRoleOwnership(message = "角色不存在或无权访问")
    public Result<TsRoleVo> editRole(LoginUser user, Long id, TsRoleSaveDto request) {
        TsRole role = TsRoleOwnershipAspect.ROLE_CONTEXT.get();
        TsRoleSavePo savePo = TsRoleSavePo.fromRequest(request);
        savePo.applyTo(role);
        role.setUpdatedAt(new Date());
        this.updateById(role);
        return Result.OK("修改成功", TsRoleVoConverter.fromEntity(role));
    }

    /**
     * 删除角色（软删除，status=0）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsRoleOwnership(message = "角色不存在或无权访问")
    public Result<?> deleteRole(LoginUser user, Long id) {
        TsRole role = TsRoleOwnershipAspect.ROLE_CONTEXT.get();
        role.setStatus(0);
        role.setUpdatedAt(new Date());
        this.updateById(role);
        return Result.OK("删除成功");
    }

    /**
     * 一键补全角色设定。
     */
    @Override
    public Result<TsRoleOneClickSettingGenerateVo> generateRoleSetting(LoginUser user, TsRoleOneClickSettingGenerateDto request) {
        return Result.OK(tsRoleGenerateService.generateRoleSetting(user, request));
    }

    /**
     * 一键生成角色形象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRoleOneClickImageGenerateVo> generateRoleImage(LoginUser user, TsRoleOneClickImageGenerateDto request) {
        return Result.OK(tsRoleGenerateService.generateRoleImage(user, request));
    }

    /**
     * 一键生成角色声音。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRoleOneClickVoiceGenerateVo> generateRoleVoice(LoginUser user, TsRoleOneClickVoiceGenerateDto request) {
        return Result.OK(tsRoleGenerateService.generateRoleVoice(user, request));
    }

    /**
     * 随机生成完整角色（设定+形象+声音）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRoleGenerateRoleVo> generateRole(LoginUser user, TsRoleGenerateRoleDto request) {
        return Result.OK(tsRoleGenerateService.generateRole(user, request));
    }
}
