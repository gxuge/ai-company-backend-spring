package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

import java.util.List;

@Data
public class TsRoleOneClickSettingGenerateVo {
    /** 角色名称 */
    private String roleName;
    /** 性别 */
    private String gender;
    /** 职业 */
    private String occupation;
    /** 背景故事 */
    private String backgroundStory;
    /** 本次由模型补全的字段列表 */
    private List<String> filledFields;
    /** 本次沿用原值的字段列表 */
    private List<String> keptFields;
    /** 使用的Prompt编码 */
    private String promptCode;
    /** 使用的Prompt版本 */
    private String promptVersion;
    /** 实际渲染后的Prompt */
    private String renderedPrompt;
    /** Redis快照Key */
    private String snapshotKey;
}
