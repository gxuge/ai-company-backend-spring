package org.jeecg.modules.system.vo.tsvoiceprofile;

import lombok.Data;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;

import java.util.Date;
import java.util.List;
@Data
public class TsVoiceProfileVo {
    /** 音色主键 ID */
    private Long id;
    /** 音色名称 */
    private String name;
    /** 提供商音色 ID */
    private String providerVoiceId;
    /** 音色头像 URL */
    private String avatarUrl;
    /** 性别标签 */
    private String gender;
    /** 年龄段标签 */
    private String ageGroup;
    /** 状态：0-禁用，1-启用 */
    private Integer status;
    /** 排序号（越小越靠前） */
    private Integer sortNo;
    /** 标签 ID 列表 */
    private List<Long> tagIds;
    /** 标签详情列表 */
    private List<TsVoiceTagVo> tags;
    /** 创建时间 */
    private Date createdAt;
    /** 更新时间 */
    private Date updatedAt;
}
