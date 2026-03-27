package org.jeecg.modules.system.po.tsroleimageprofile;

import lombok.Data;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileSaveDto;
import org.jeecg.modules.system.entity.TsRoleImageProfile;
@Data
public class TsRoleImageProfileSavePo {
    private String profileName;
    private String promptText;
    private String styleName;
    private Long selectedImageAssetId;
    private String selectedImageUrl;
    private String sourceType;
    private Integer isPublic;
    private Integer status;
    private String extJson;
    public static TsRoleImageProfileSavePo fromRequest(TsRoleImageProfileSaveDto request) {
        TsRoleImageProfileSavePo po = new TsRoleImageProfileSavePo();
        if (request == null) {
            return po;
        }
        po.setProfileName(trimToNull(request.getProfileName()));
        po.setPromptText(request.getPromptText());
        po.setStyleName(trimToNull(request.getStyleName()));
        po.setSelectedImageAssetId(request.getSelectedImageAssetId());
        po.setSelectedImageUrl(trimToNull(request.getSelectedImageUrl()));
        po.setSourceType(trimToNull(request.getSourceType()));
        po.setIsPublic(request.getIsPublic());
        po.setStatus(request.getStatus());
        po.setExtJson(request.getExtJson());
        return po;
    }
    public void applyTo(TsRoleImageProfile entity) {
        if (entity == null) {
            return;
        }
        entity.setProfileName(this.profileName);
        entity.setPromptText(this.promptText);
        entity.setStyleName(this.styleName);
        entity.setSelectedImageAssetId(this.selectedImageAssetId);
        entity.setSelectedImageUrl(this.selectedImageUrl);
        entity.setSourceType(this.sourceType);
        entity.setIsPublic(this.isPublic);
        entity.setStatus(this.status);
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