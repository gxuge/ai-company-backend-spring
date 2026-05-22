package org.jeecg.modules.system.dto.tsrole;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ImageGenerateRuntimeResult {
    private String renderedPrompt;
    private JSONObject modelJson;
    private String imagePrompt;
    private String imageUrl;
    private Long assetId;
}
