package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

/**
 * 随机完整角色生成结果。
 */
@Data
public class TsRoleGenerateRoleVo {
    /** 新创建角色ID */
    private Long roleId;
    /** 角色设定生成结果 */
    private TsRoleOneClickSettingGenerateVo settingResult;
    /** 角色形象生成结果 */
    private TsRoleOneClickImageGenerateVo imageResult;
    /** 角色声音生成结果 */
    private TsRoleOneClickVoiceGenerateVo voiceResult;
    /** 使用的Prompt编码 */
    private String promptCode;
    /** 使用的Prompt版本 */
    private String promptVersion;
    /** 实际渲染后的Prompt */
    private String renderedPrompt;
    /** Redis快照Key */
    private String snapshotKey;
}
