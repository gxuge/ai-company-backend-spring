package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Data
public class TsStorySaveDto {
    public interface Create {}
    public interface Update {}
    @NotNull(message = "编辑故事时id不能为空", groups = Update.class)
    private Long id;
    @NotBlank(message = "故事标题不能为空", groups = {Create.class, Update.class})
    private String title;
    private String storyIntro;
    private String storyMode;
    private String storySetting;
    private String storyBackground;
    private String coverUrl;
    private Long sceneId;
    private String sceneNameSnapshot;
    private Integer status;
    private Integer isPublic;
    private Integer isAiStorySetting;
    private Integer isAiCharacter;
    private Integer isAiOutline;
    private String remark;
    private List<TsStoryRoleBindingDto> roleBindings;
    public void applyCreateDefaults() {
        if (this.storyMode == null || this.storyMode.trim().isEmpty()) {
            this.storyMode = "chapter";
        }
        if (this.status == null) {
            this.status = 0;
        }
        if (this.isPublic == null) {
            this.isPublic = 1;
        }
        if (this.isAiStorySetting == null) {
            this.isAiStorySetting = 0;
        }
        if (this.isAiCharacter == null) {
            this.isAiCharacter = 0;
        }
        if (this.isAiOutline == null) {
            this.isAiOutline = 0;
        }
    }
    public List<TsStoryRoleBindingDto> normalizeRoleBindings() {
        if (this.roleBindings == null) {
            return null;
        }
        Map<Long, TsStoryRoleBindingDto> uniqueMap = new LinkedHashMap<>();
        for (TsStoryRoleBindingDto item : this.roleBindings) {
            if (item == null || item.getRoleId() == null || item.getRoleId() <= 0) {
                continue;
            }
            TsStoryRoleBindingDto normalized = new TsStoryRoleBindingDto();
            normalized.setRoleId(item.getRoleId());

            String roleType = trimToNull(item.getRoleType());
            normalized.setRoleType(roleType == null ? "support" : roleType);

            normalized.setSortNo(item.getSortNo() == null ? 0 : item.getSortNo());

            Integer isRequired = item.getIsRequired();
            normalized.setIsRequired(isRequired != null && isRequired == 0 ? 0 : 1);

            String joinSource = trimToNull(item.getJoinSource());
            normalized.setJoinSource(joinSource == null ? "manual" : joinSource);

            uniqueMap.put(normalized.getRoleId(), normalized);
        }
        return new ArrayList<>(uniqueMap.values());
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}