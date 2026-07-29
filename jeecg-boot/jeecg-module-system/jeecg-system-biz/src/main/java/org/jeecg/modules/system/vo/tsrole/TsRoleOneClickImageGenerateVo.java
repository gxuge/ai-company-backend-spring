package org.jeecg.modules.system.vo.tsrole;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TsRoleOneClickImageGenerateVo {
    /** 生成结果图片URL */
    private String imageUrl;
    /** 使用的Prompt编码 */
    private String promptCode;
    /** 使用的Prompt版本 */
    private String promptVersion;
    /** Redis快照Key */
    @JsonIgnore
    private transient String snapshotKey;
}
