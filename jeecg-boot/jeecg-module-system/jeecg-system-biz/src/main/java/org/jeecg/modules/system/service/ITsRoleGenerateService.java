package org.jeecg.modules.system.service;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateRoleDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickVoiceGenerateDto;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickSettingGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickVoiceGenerateVo;

/**
 * 角色一键生成服务。
 */
public interface ITsRoleGenerateService {
    /**
     * 一键补全角色设定（四核心字段）。
     */
    TsRoleOneClickSettingGenerateVo generateRoleSetting(LoginUser user, TsRoleOneClickSettingGenerateDto request);

    /**
     * 一键生成角色形象。
     */
    TsRoleOneClickImageGenerateVo generateRoleImage(LoginUser user, TsRoleOneClickImageGenerateDto request);

    /**
     * 一键生成角色声音。
     */
    TsRoleOneClickVoiceGenerateVo generateRoleVoice(LoginUser user, TsRoleOneClickVoiceGenerateDto request);

    /**
     * 生成完整角色（设定+形象+声音）。
     */
    TsRoleGenerateRoleVo generateRole(LoginUser user, TsRoleGenerateRoleDto request);
}
