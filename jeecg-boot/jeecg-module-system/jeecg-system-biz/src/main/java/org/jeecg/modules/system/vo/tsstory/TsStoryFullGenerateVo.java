package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

@Data
public class TsStoryFullGenerateVo {
    /** 核心字段：标题。 */
    private String title;
    /** 核心字段：故事模式。 */
    private String storyMode;
    /** 核心字段：故事简介。 */
    private String storyIntro;
    /** 核心字段：故事设定。 */
    private String storySetting;
    /** 核心字段：场景设定（site_setting）。 */
    private String siteSetting;
    /** 核心字段：剧情大纲（plot_outline）。 */
    private String plotOutline;
}
