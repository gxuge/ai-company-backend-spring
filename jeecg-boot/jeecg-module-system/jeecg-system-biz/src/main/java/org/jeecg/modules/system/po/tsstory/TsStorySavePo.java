package org.jeecg.modules.system.po.tsstory;

import lombok.Data;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsstory.TsStorySaveDto;
import org.jeecg.modules.system.entity.TsStory;

import java.util.Date;
@Data
public class TsStorySavePo {
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
    public static TsStorySavePo fromRequest(TsStorySaveDto request) {
        TsStorySavePo po = new TsStorySavePo();
        if (request == null) {
            return po;
        }
        po.setTitle(trimToNull(request.getTitle()));
        po.setStoryIntro(trimToNull(request.getStoryIntro()));
        po.setStoryMode(trimToNull(request.getStoryMode()));
        po.setStorySetting(request.getStorySetting());
        po.setStoryBackground(request.getStoryBackground());
        po.setCoverUrl(trimToNull(request.getCoverUrl()));
        po.setSceneId(request.getSceneId());
        po.setSceneNameSnapshot(trimToNull(request.getSceneNameSnapshot()));
        po.setStatus(request.getStatus());
        po.setIsPublic(request.getIsPublic());
        po.setIsAiStorySetting(request.getIsAiStorySetting());
        po.setIsAiCharacter(request.getIsAiCharacter());
        po.setIsAiOutline(request.getIsAiOutline());
        po.setRemark(trimToNull(request.getRemark()));
        return po;
    }
    public void applyTo(TsStory story) {
        if (story == null) {
            return;
        }
        story.setTitle(this.title);
        story.setStoryIntro(this.storyIntro);
        story.setStoryMode(this.storyMode);
        story.setStorySetting(this.storySetting);
        story.setStoryBackground(this.storyBackground);
        story.setCoverUrl(this.coverUrl);
        story.setSceneId(this.sceneId);
        story.setSceneNameSnapshot(this.sceneNameSnapshot);
        story.setStatus(this.status);
        story.setIsPublic(this.isPublic);
        story.setIsAiStorySetting(this.isAiStorySetting);
        story.setIsAiCharacter(this.isAiCharacter);
        story.setIsAiOutline(this.isAiOutline);
        story.setRemark(this.remark);
    }
    public void applyForCreate(TsStory story, LoginUser user, String userId, String storyCode, Date now) {
        applyTo(story);
        story.setUserId(userId);
        story.setStoryCode(storyCode);
        story.setCreatedBy(userId);
        story.setCreatedName(user == null ? null : user.getRealname());
        story.setUpdatedBy(userId);
        story.setUpdatedName(user == null ? null : user.getRealname());
        story.setIsDeleted(0);
        story.setCreatedAt(now);
        story.setUpdatedAt(now);
    }
    public void applyForUpdate(TsStory story, LoginUser user, String userId, Date now) {
        applyTo(story);
        story.setUpdatedBy(userId);
        story.setUpdatedName(user == null ? null : user.getRealname());
        story.setUpdatedAt(now);
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}