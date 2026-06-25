package org.jeecg.modules.system.vo.tsimage;

import lombok.Data;

/**
 * 统一图片语义资源描述。
 *
 * <p>当前阶段优先兼容现有 ts_* 接口中的 URL 字段，后续若接入独立图片资源表，
 * 可继续补充 imageId/version/hash 等字段，而不必改动业务接口结构。</p>
 */
@Data
public class TsImageResourceVo {
    private String imageType;
    private String url;
    private String sourceField;
    private String variant;
    private String privacy;
    private String userId;
    private Long characterId;
    private Long storyId;
    private Long sceneId;
    private Long sourceImageId;
}
