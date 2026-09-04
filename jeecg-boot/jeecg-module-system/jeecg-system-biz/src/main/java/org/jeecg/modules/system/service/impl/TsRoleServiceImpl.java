package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsRoleOwnershipAspect;
import org.jeecg.modules.aop.TsRoleOwnershipAspect.CheckTsRoleOwnership;
import org.jeecg.modules.system.activity.TsActivityProgressReporter;
import org.jeecg.modules.system.behavior.TsBehaviorEventReporter;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateImageByPromptDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateRoleDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateImagePromptByTemplateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateTextByTemplateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleImagePromptOptimizeDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickVoiceGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleQueryDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleSaveDto;
import org.jeecg.modules.system.constant.TsWorkReviewConstants;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.enums.tsbehavior.TsBehaviorEventType;
import org.jeecg.modules.system.enums.tsactivity.TsActivityConditionType;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.po.tsrole.TsRoleQueryPo;
import org.jeecg.modules.system.po.tsrole.TsRoleSavePo;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.service.ITsRoleService;
import org.jeecg.modules.system.service.ITsContentTagService;
import org.jeecg.modules.system.service.ITsWorkReviewService;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateImageByPromptVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateImagePromptByTemplateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateTextByTemplateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleImagePromptOptimizeVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickSettingGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickVoiceGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleVoConverter;
import org.jeecg.modules.system.vo.tscontenttag.TsContentTagDisplayVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TsRoleServiceImpl extends ServiceImpl<TsRoleMapper, TsRole> implements ITsRoleService {
    @Resource
    private ITsRoleGenerateService tsRoleGenerateService;
    @Resource
    private ITsWorkReviewService tsWorkReviewService;
    @Resource
    private ITsContentTagService tsContentTagService;
    @Resource
    private TsActivityProgressReporter activityProgressReporter;
    @Resource
    private TsBehaviorEventReporter behaviorEventReporter;

    /**
     * 分页查询当前用户角色列表。
     */
    @Override
    public Result<Page<TsRoleVo>> pageRoles(LoginUser user, TsRoleQueryDto request) {
        String userId = user.getId();
        TsRoleQueryPo queryPo = TsRoleQueryPo.fromRequest(userId, request);
        Page<TsRole> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsRole> pageData = baseMapper.selectRolePage(page, queryPo);
        Page<TsRoleVo> voPage = TsRoleVoConverter.fromPage(pageData);
        enrichRoleTags(voPage.getRecords());
        return Result.OK(voPage);
    }

    /**
     * 查询角色详情，归属校验由 AOP 完成。
     */
    @Override
    @CheckTsRoleOwnership(message = "角色不存在或无权访问")
    public Result<TsRoleVo> getRole(LoginUser user, Long id) {
        TsRole role = TsRoleOwnershipAspect.ROLE_CONTEXT.get();
        return Result.OK(enrichRoleTags(TsRoleVoConverter.fromEntity(role)));
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
        role.setContentVersion(0);
        role.setReviewStatus(TsWorkReviewConstants.PENDING_AI);
        role.setDesiredPublic(request.getIsPublic());
        role.setCreatedAt(new Date());
        role.setUpdatedAt(new Date());
        this.save(role);
        tsContentTagService.replaceTags(
                "role", role.getId(), role.getContentVersion() + 1, null,
                "generation", request.getTagModelVersion(), request.getTags(), true);
        tsWorkReviewService.submitRole(role.getId(), request.getIsPublic());
        activityProgressReporter.reportAfterCommit(
                userId,
                TsActivityConditionType.ROLE_CREATE,
                "role-create:" + role.getId());
        behaviorEventReporter.reportAfterCommit(
                userId,
                TsBehaviorEventType.ROLE_CREATE,
                "role",
                role.getId(),
                role.getGender() == null
                        ? Map.of() : Map.of("gender", role.getGender()));
        return Result.OK("新增成功", enrichRoleTags(
                TsRoleVoConverter.fromEntity(this.getById(role.getId()))));
    }

    /**
     * 编辑角色基础信息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsRoleOwnership(message = "角色不存在或无权访问")
    public Result<TsRoleVo> editRole(LoginUser user, Long id, TsRoleSaveDto request) {
        TsRole role = TsRoleOwnershipAspect.ROLE_CONTEXT.get();
        Integer requestedPublic = request.getIsPublic() != null
                ? request.getIsPublic()
                : (role.getDesiredPublic() != null ? role.getDesiredPublic() : role.getIsPublic());
        TsRoleSavePo savePo = TsRoleSavePo.fromRequest(request);
        savePo.applyTo(role);
        role.setUpdatedAt(new Date());
        this.updateById(role);
        tsContentTagService.replaceTags(
                "role", role.getId(), role.getContentVersion() + 1, null,
                "generation", request.getTagModelVersion(), request.getTags(), true);
        tsWorkReviewService.submitRole(role.getId(), requestedPublic);
        return Result.OK("修改成功", enrichRoleTags(
                TsRoleVoConverter.fromEntity(this.getById(role.getId()))));
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

    /** 为角色响应批量补充当前内容版本标签。 */
    private void enrichRoleTags(List<TsRoleVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, Integer> versions = new LinkedHashMap<>();
        for (TsRoleVo item : records) {
            if (item != null && item.getId() != null && item.getContentVersion() != null) {
                versions.put(item.getId(), item.getContentVersion());
            }
        }
        Map<Long, List<TsContentTagDisplayVo>> tags =
                tsContentTagService.findCurrentDisplayTags("role", versions);
        for (TsRoleVo item : records) {
            if (item != null) {
                item.setTags(tags.getOrDefault(item.getId(), List.of()));
            }
        }
    }

    /** 为单个角色响应补充当前内容版本标签。 */
    private TsRoleVo enrichRoleTags(TsRoleVo item) {
        if (item != null) {
            enrichRoleTags(List.of(item));
        }
        return item;
    }

    /**
     * 一键补全角色设定。
     */
    @Override
    public Result<TsRoleOneClickSettingGenerateVo> generateRoleSetting(LoginUser user, TsRoleOneClickSettingGenerateDto request) {
        return Result.OK(tsRoleGenerateService.generateRoleSetting(user, request));
    }

    @Override
    public Result<TsRoleOneClickSettingGenerateVo> generateRoleSettingPreset(LoginUser user, TsRoleOneClickSettingGenerateDto request) {
        return Result.OK(tsRoleGenerateService.generateRoleSettingPreset(user, request));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRoleGenerateImageByPromptVo> generateImageByPrompt(LoginUser user, TsRoleGenerateImageByPromptDto request) {
        return Result.OK(tsRoleGenerateService.generateImageByPrompt(user, request));
    }

    @Override
    public Result<TsRoleGenerateTextByTemplateVo> generateTextByTemplate(LoginUser user, TsRoleGenerateTextByTemplateDto request) {
        return Result.OK(tsRoleGenerateService.generateTextByTemplate(user, request));
    }

    @Override
    public Result<TsRoleGenerateImagePromptByTemplateVo> generateImagePromptByTemplate(LoginUser user, TsRoleGenerateImagePromptByTemplateDto request) {
        return Result.OK(tsRoleGenerateService.generateImagePromptByTemplate(user, request));
    }

    @Override
    public Result<TsRoleImagePromptOptimizeVo> optimizeRoleImagePrompt(LoginUser user, TsRoleImagePromptOptimizeDto request) {
        return Result.OK(tsRoleGenerateService.optimizeRoleImagePrompt(user, request));
    }
}
