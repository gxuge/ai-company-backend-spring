package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
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
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateImageByPromptVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateImagePromptByTemplateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateTextByTemplateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleImagePromptOptimizeVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickSettingGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickVoiceGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleVo;

public interface ITsRoleService extends IService<TsRole> {
    /**
     * 分页查询角色列表。
     */
    Result<Page<TsRoleVo>> pageRoles(LoginUser user, TsRoleQueryDto request);

    /**
     * 查询角色详情。
     */
    Result<TsRoleVo> getRole(LoginUser user, Long id);

    /**
     * 新增角色。
     */
    Result<TsRoleVo> addRole(LoginUser user, TsRoleSaveDto request);

    /**
     * 编辑角色。
     */
    Result<TsRoleVo> editRole(LoginUser user, Long id, TsRoleSaveDto request);

    /**
     * 删除角色（软删除）。
     */
    Result<?> deleteRole(LoginUser user, Long id);

    /**
     * 一键补全角色设定。
     */
    Result<TsRoleOneClickSettingGenerateVo> generateRoleSetting(LoginUser user, TsRoleOneClickSettingGenerateDto request);

    /**
     * 一键补全角色设定（预设版）。
     */
    Result<TsRoleOneClickSettingGenerateVo> generateRoleSettingPreset(LoginUser user, TsRoleOneClickSettingGenerateDto request);

    /**
     * 一键生成角色形象。
     */
    Result<TsRoleOneClickImageGenerateVo> generateRoleImage(LoginUser user, TsRoleOneClickImageGenerateDto request);

    /**
     * 一键生成角色声音。
     */
    Result<TsRoleOneClickVoiceGenerateVo> generateRoleVoice(LoginUser user, TsRoleOneClickVoiceGenerateDto request);

    /**
     * 生成完整角色（设定+形象+声音）。
     */
    Result<TsRoleGenerateRoleVo> generateRole(LoginUser user, TsRoleGenerateRoleDto request);

    /**
     * 直接调用生图模型生成角色形象。
     */
    Result<TsRoleGenerateImageByPromptVo> generateImageByPrompt(LoginUser user, TsRoleGenerateImageByPromptDto request);

    /**
     * 基于模板渲染并生成一段文本。
     */
    Result<TsRoleGenerateTextByTemplateVo> generateTextByTemplate(LoginUser user, TsRoleGenerateTextByTemplateDto request);

    /**
     * 基于模板生成角色形象提示词。
     */
    Result<TsRoleGenerateImagePromptByTemplateVo> generateImagePromptByTemplate(LoginUser user, TsRoleGenerateImagePromptByTemplateDto request);

    /**
     * 角色形象提示词优化。
     */
    Result<TsRoleImagePromptOptimizeVo> optimizeRoleImagePrompt(LoginUser user, TsRoleImagePromptOptimizeDto request);
}
