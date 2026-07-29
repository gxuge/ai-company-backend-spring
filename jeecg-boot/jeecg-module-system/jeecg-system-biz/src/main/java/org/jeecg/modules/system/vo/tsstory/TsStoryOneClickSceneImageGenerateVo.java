package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

/**
 * 故事场景背景图片生成结果。
 */
@Data
public class TsStoryOneClickSceneImageGenerateVo {
    /** AI供应商返回的原始图片地址。 */
    private String imageUrl;
    /** 本次使用的提示词模板编码。 */
    private String promptCode;
    /** 本次使用的提示词模板版本。 */
    private String promptVersion;
}
