package org.jeecg.modules.openapi.vo;

import lombok.Data;

import java.util.List;

/**
 * MiniMax 文生图响应 VO。
 */
@Data
public class MiniMaxImageResponseVo {

    /**
     * 图片地址列表。
     */
    private List<String> imageUrls;
    private List<String> originalImageUrls;
}
