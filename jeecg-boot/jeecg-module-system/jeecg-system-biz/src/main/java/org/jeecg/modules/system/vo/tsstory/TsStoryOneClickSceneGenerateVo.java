package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

import java.util.List;

@Data
public class TsStoryOneClickSceneGenerateVo {
    private String sceneNameSnapshot;
    private String sceneSummary;
    private List<String> sceneElements;
    private String promptCode;
    private String promptVersion;
    private String renderedPrompt;
    private String snapshotKey;
}
