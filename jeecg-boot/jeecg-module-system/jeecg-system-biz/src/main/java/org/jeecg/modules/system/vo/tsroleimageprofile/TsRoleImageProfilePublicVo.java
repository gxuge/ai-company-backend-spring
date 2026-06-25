package org.jeecg.modules.system.vo.tsroleimageprofile;

import lombok.Data;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceVo;

import java.util.Date;
import java.util.Map;

@Data
public class TsRoleImageProfilePublicVo {
    private Long id;
    private String profileName;
    private String styleName;
    private String selectedImageUrl;
    private String sourceType;
    private String promptText;
    private String authorName;
    private String authorAvatar;
    private Date updatedAt;
    private Map<String, TsImageResourceVo> imageResources;
}
