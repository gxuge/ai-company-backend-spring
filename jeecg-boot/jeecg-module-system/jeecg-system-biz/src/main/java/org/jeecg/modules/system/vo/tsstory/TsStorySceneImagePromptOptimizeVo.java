package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

/**
 * 故事场景图片提示词润色结果。
 */
@Data
public class TsStorySceneImagePromptOptimizeVo {
    /** 润色后的场景视觉提示词。 */
    private String visualPrompt;
    /** 生图负面提示词。 */
    private String negativePrompt;
    /** 本次使用的提示词模板编码。 */
    private String promptCode;
    /** 本次使用的提示词模板版本。 */
    private String promptVersion;
    /** 实际渲染后的模型提示词。 */
    private String renderedPrompt;
    /** Redis 快照键。 */
    private String snapshotKey;
}
