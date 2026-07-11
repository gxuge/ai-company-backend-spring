package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

@Data
public class TsRoleImagePromptOptimizeVo {
    private String visualPrompt;
    private String negativePrompt;
    private String promptCode;
    private String promptVersion;
    private String renderedPrompt;
    private String snapshotKey;
}
