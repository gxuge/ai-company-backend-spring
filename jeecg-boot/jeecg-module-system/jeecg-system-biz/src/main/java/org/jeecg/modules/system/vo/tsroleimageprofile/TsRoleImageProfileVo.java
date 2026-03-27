package org.jeecg.modules.system.vo.tsroleimageprofile;

import lombok.Data;

import java.util.Date;
@Data
public class TsRoleImageProfileVo {
    private Long id;
    private String profileName;
    private String ownerUserId;
    private String promptText;
    private String styleName;
    private Long selectedImageAssetId;
    private String selectedImageUrl;
    private String sourceType;
    private Integer isPublic;
    private Integer status;
    private String extJson;
    private Date createdAt;
    private Date updatedAt;
}