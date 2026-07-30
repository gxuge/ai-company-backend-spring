package org.jeecg.modules.system.dto.tsdraft;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 统一草稿保存参数。
 */
@Data
public class TsDraftSaveDto {

    /** 新增校验分组。 */
    public interface Create {
    }

    /** 更新校验分组。 */
    public interface Update {
    }

    /** 草稿主键，更新时必填。 */
    @NotNull(message = "编辑草稿时id不能为空", groups = Update.class)
    private Long id;

    /** 草稿类型：role 角色，story 故事。 */
    @NotBlank(message = "草稿类型不能为空", groups = {Create.class, Update.class})
    @Pattern(regexp = "^(role|story)$", message = "草稿类型仅支持role或story",
            groups = {Create.class, Update.class})
    private String draftType;

    /** 草稿箱展示名称。 */
    @NotBlank(message = "草稿名称不能为空", groups = {Create.class, Update.class})
    @Size(max = 200, message = "草稿名称长度不能超过200个字符",
            groups = {Create.class, Update.class})
    private String draftName;

    /** 来源正式资源 ID，可为空。 */
    @Positive(message = "来源资源ID必须为正整数", groups = {Create.class, Update.class})
    private Long sourceId;

    /** 页面完整状态对象。 */
    @NotNull(message = "草稿内容不能为空", groups = {Create.class, Update.class})
    private Map<String, Object> content;
}
