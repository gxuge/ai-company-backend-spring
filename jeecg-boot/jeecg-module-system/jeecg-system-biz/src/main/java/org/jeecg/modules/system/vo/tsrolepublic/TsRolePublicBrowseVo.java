package org.jeecg.modules.system.vo.tsrolepublic;

import lombok.Data;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceVo;

import java.util.Date;
import java.util.Map;

/**
 * 公开角色浏览展示对象。
 */
@Data
public class TsRolePublicBrowseVo {
    /** 角色ID。 */
    private Long id;
    /** 公开记录ID。 */
    private Long publicId;
    /** 渠道编码。 */
    private String channelCode;
    /** 角色名称。 */
    private String roleName;
    /** 角色副标题。 */
    private String roleSubtitle;
    /** 头像。 */
    private String avatarUrl;
    /** 封面。 */
    private String coverUrl;
    /** 性别。 */
    private String gender;
    /** 职业。 */
    private String occupation;
    /** 展示简介。 */
    private String introText;
    /** 作者名称。 */
    private String authorName;
    /** 作者头像。 */
    private String authorAvatar;
    /** 更新时间。 */
    private Date updatedAt;
    /** 统一图片语义资源。 */
    private Map<String, TsImageResourceVo> imageResources;
}
