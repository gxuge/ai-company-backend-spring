package org.jeecg.modules.system.vo.tsstorypublic;

import lombok.Data;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceVo;

import java.util.Date;
import java.util.Map;

/**
 * 公开故事浏览展示对象。
 */
@Data
public class TsStoryPublicBrowseVo {
    /** 故事ID。 */
    private Long id;
    /** 公开记录ID。 */
    private Long publicId;
    /** 渠道编码。 */
    private String channelCode;
    /** 标题。 */
    private String title;
    /** 展示副标题。 */
    private String displaySubtitle;
    /** 故事简介。 */
    private String storyIntro;
    /** 故事模式。 */
    private String storyMode;
    /** 封面图。 */
    private String coverUrl;
    /** 场景图。 */
    private String sceneImageUrl;
    /** 关注人数。 */
    private Long followerCount;
    /** 对话人数。 */
    private Long dialogueCount;
    /** 作者名称。 */
    private String authorName;
    /** 作者头像。 */
    private String authorAvatar;
    /** 更新时间。 */
    private Date updatedAt;
    /** 统一图片语义资源。 */
    private Map<String, TsImageResourceVo> imageResources;
}
