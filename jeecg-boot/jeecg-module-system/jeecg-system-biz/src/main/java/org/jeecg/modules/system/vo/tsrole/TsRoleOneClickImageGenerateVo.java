package org.jeecg.modules.system.vo.tsrole;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TsRoleOneClickImageGenerateVo {
    /** 是否已受理 */
    private Boolean accepted;
    /** 生成状态（pending/running/success/failed） */
    private String generateStatus;
    /** 生成结果图片URL */
    private String imageUrl;
    /** 图片资产ID */
    private Long imageAssetId;
    /** 生图记录ID */
    private Long generateRecordId;
    /** 最终用于文生图的提示词 */
    private String imagePrompt;
    /** 使用的Prompt编码 */
    private String promptCode;
    /** 使用的Prompt版本 */
    private String promptVersion;
    /** 实际渲染后的Prompt */
    private String renderedPrompt;
    /** Redis快照Key */
    @JsonIgnore
    private transient String snapshotKey;
}
