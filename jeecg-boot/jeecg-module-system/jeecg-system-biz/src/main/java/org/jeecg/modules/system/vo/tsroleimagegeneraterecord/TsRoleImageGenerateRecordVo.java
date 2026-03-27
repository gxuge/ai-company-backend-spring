package org.jeecg.modules.system.vo.tsroleimagegeneraterecord;

import lombok.Data;

import java.util.Date;
@Data
public class TsRoleImageGenerateRecordVo {
    private Long id;
    private Long roleId;
    private String sourceProfileUrl;
    private String userId;
    private String promptText;
    private String styleName;
    private String referenceAssetsJson;
    private String generateStatus;
    private String applyStatus;
    private Long resultAssetId;
    private String resultImageUrl;
    private String failReason;
    private String requestId;
    private String extJson;
    private Date createdAt;
    private Date updatedAt;
}