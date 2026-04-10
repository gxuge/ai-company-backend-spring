package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

@Data
public class TsRoleAuthorPublicVo {
    private String userId;
    private String displayName;
    private String avatar;
    private Integer verified;
    private String bio;
}
