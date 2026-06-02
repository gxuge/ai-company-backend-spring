package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

import java.util.List;

@Data
public class TsStoryFullGenerateVo {
    /** 核心字段：标题。 */
    private String title;
    /** 核心字段：故事简介。 */
    private String storyIntro;
    /** 核心字段：故事设定。 */
    private String storySetting;
    /** 核心字段：场景设定（site_setting）。 */
    private String siteSetting;
    /** 核心字段：剧情大纲（plot_outline）。 */
    private String plotOutline;

    private String presetId;
    private String presetName;
    private String presetDescription;

    /** 模板变量名，当前固定为 value。 */
    private String variableName;
    /** 变量填充值（多个标签值逗号拼接）。 */
    private String variableValue;
    /** 用户模板原文。 */
    private String templateText;
    /** 完成变量替换后的模板文本。 */
    private String filledTemplateText;

    private String promptCode;
    private String promptVersion;
    private String renderedPrompt;
    private String snapshotKey;

    private List<TsStoryFullGeneratePresetTagVo> presetTags;

    /** 与现有故事设定生成接口一致。 */
    private TsStoryOneClickSettingGenerateVo settingResult;
    /** 与现有场景生成接口一致。 */
    private TsStoryOneClickSceneGenerateVo sceneResult;
    /** 与现有大纲生成接口一致。 */
    private TsStoryOneClickOutlineGenerateVo outlineResult;

    private Boolean outlineSkipped;
    private String outlineSkippedReason;
}
