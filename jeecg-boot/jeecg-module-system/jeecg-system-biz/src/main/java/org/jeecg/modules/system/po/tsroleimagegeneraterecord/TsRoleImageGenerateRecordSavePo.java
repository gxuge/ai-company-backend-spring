package org.jeecg.modules.system.po.tsroleimagegeneraterecord;

import lombok.Data;
import org.jeecg.modules.system.dto.tsroleimagegeneraterecord.TsRoleImageGenerateRecordSaveDto;
import org.jeecg.modules.system.entity.TsRoleImageGenerateRecord;
@Data
public class TsRoleImageGenerateRecordSavePo {
    private String sourceProfileUrl;
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
    public static TsRoleImageGenerateRecordSavePo fromRequest(TsRoleImageGenerateRecordSaveDto request) {
        TsRoleImageGenerateRecordSavePo po = new TsRoleImageGenerateRecordSavePo();
        if (request == null) {
            return po;
        }
        po.setSourceProfileUrl(trimToNull(request.getSourceProfileUrl()));
        po.setPromptText(request.getPromptText());
        po.setStyleName(trimToNull(request.getStyleName()));
        po.setReferenceAssetsJson(request.getReferenceAssetsJson());
        po.setGenerateStatus(trimToNull(request.getGenerateStatus()));
        po.setApplyStatus(trimToNull(request.getApplyStatus()));
        po.setResultAssetId(request.getResultAssetId());
        po.setResultImageUrl(trimToNull(request.getResultImageUrl()));
        po.setFailReason(trimToNull(request.getFailReason()));
        po.setRequestId(trimToNull(request.getRequestId()));
        po.setExtJson(request.getExtJson());
        return po;
    }
    public void applyTo(TsRoleImageGenerateRecord entity) {
        if (entity == null) {
            return;
        }
        entity.setSourceProfileUrl(this.sourceProfileUrl);
        entity.setPromptText(this.promptText);
        entity.setStyleName(this.styleName);
        entity.setReferenceAssetsJson(this.referenceAssetsJson);
        entity.setGenerateStatus(this.generateStatus);
        entity.setApplyStatus(this.applyStatus);
        entity.setResultAssetId(this.resultAssetId);
        entity.setResultImageUrl(this.resultImageUrl);
        entity.setFailReason(this.failReason);
        entity.setRequestId(this.requestId);
        entity.setExtJson(this.extJson);
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}