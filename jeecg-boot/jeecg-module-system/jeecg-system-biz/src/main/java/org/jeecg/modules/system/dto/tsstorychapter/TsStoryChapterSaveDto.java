package org.jeecg.modules.system.dto.tsstorychapter;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
@Data
public class TsStoryChapterSaveDto {
    public interface Create {}
    public interface Update {}
    @NotNull(message = "编辑章节时id不能为空", groups = Update.class)
    private Long id;
    @NotNull(message = "storyId不能为空", groups = {Create.class, Update.class})
    private Long storyId;
    private Integer chapterNo;
    private String chapterTitle;
    private String chapterDesc;
    private String openingContent;
    private Long openingRoleId;
    private String missionTarget;
    private Integer status;
    private Integer isAiGenerated;
    private Integer sortNo;
    private List<Long> forbiddenRoleIds;
    public void applyCreateDefaults() {
        if (this.status == null) {
            this.status = 1;
        }
        if (this.isAiGenerated == null) {
            this.isAiGenerated = 0;
        }
        if (this.sortNo == null) {
            this.sortNo = 0;
        }
    }
    public List<Long> normalizeForbiddenRoleIds() {
        if (this.forbiddenRoleIds == null || this.forbiddenRoleIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> uniqueRoleIds = new LinkedHashSet<>();
        for (Long roleId : this.forbiddenRoleIds) {
            if (roleId != null && roleId > 0) {
                uniqueRoleIds.add(roleId);
            }
        }
        return new ArrayList<>(uniqueRoleIds);
    }
}