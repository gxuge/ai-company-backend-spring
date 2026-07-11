package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

import java.util.List;

@Data
public class TsRoleGenerateImageByPromptVo {
    private String promptUsed;
    private String styleUsed;
    private String referenceImageUrl;
    private List<String> originalImageUrls;
    private List<String> imageUrls;
    private String snapshotKey;
}
