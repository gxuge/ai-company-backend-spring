package org.jeecg.modules.system.vo.tsimage;

import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将现有 ts_* 接口中的不同图片字段名映射为统一图片语义。
 */
public final class TsImageResourceResolver {

    public static final String IMAGE_TYPE_USER_AVATAR = "user_avatar";
    public static final String IMAGE_TYPE_CHARACTER_IMAGE = "character_image";
    public static final String IMAGE_TYPE_CHARACTER_AVATAR = "character_avatar";
    public static final String IMAGE_TYPE_STORY_SCENE = "story_scene";
    public static final String IMAGE_TYPE_STORY_COVER = "story_cover";

    private TsImageResourceResolver() {
    }

    public static Map<String, TsImageResourceVo> buildRoleImageResources(Long characterId,
                                                                         String ownerUserId,
                                                                         String avatarUrl,
                                                                         String coverUrl) {
        Map<String, TsImageResourceVo> resources = new LinkedHashMap<>();
        putCharacterAvatar(resources, avatarUrl, "avatarUrl", characterId, null, ownerUserId);
        putCharacterImage(resources, coverUrl, "coverUrl", characterId, null, ownerUserId);
        return immutable(resources);
    }

    public static Map<String, TsImageResourceVo> buildStoryImageResources(Long storyId,
                                                                          Long sceneId,
                                                                          String ownerUserId,
                                                                          String sceneImageUrl,
                                                                          String coverUrl) {
        Map<String, TsImageResourceVo> resources = new LinkedHashMap<>();
        putStoryScene(resources, sceneImageUrl, "sceneImageUrl", storyId, sceneId, ownerUserId);
        put(resources, IMAGE_TYPE_STORY_COVER, coverUrl, "coverUrl", "medium", null, ownerUserId, null, storyId, null, null);
        return immutable(resources);
    }

    public static Map<String, TsImageResourceVo> buildChatSessionImageResources(Long characterId,
                                                                                String roleAvatarUrl) {
        Map<String, TsImageResourceVo> resources = new LinkedHashMap<>();
        putCharacterAvatar(resources, roleAvatarUrl, "roleAvatarUrl", characterId, null, null);
        return immutable(resources);
    }

    public static Map<String, TsImageResourceVo> buildRolePublicBrowseImageResources(Long characterId,
                                                                                     String avatarUrl,
                                                                                     String coverUrl,
                                                                                     String authorAvatar) {
        Map<String, TsImageResourceVo> resources = new LinkedHashMap<>();
        putCharacterAvatar(resources, avatarUrl, "avatarUrl", characterId, null, null);
        putCharacterImage(resources, coverUrl, "coverUrl", characterId, null, null);
        putUserAvatar(resources, authorAvatar, "authorAvatar", null, "public");
        return immutable(resources);
    }

    public static Map<String, TsImageResourceVo> buildStoryPublicBrowseImageResources(Long storyId,
                                                                                      String sceneImageUrl,
                                                                                      String coverUrl,
                                                                                      String authorAvatar) {
        Map<String, TsImageResourceVo> resources = new LinkedHashMap<>();
        putStoryScene(resources, sceneImageUrl, "sceneImageUrl", storyId, null, null);
        put(resources, IMAGE_TYPE_STORY_COVER, coverUrl, "coverUrl", "medium", null, null, null, storyId, null, null);
        putUserAvatar(resources, authorAvatar, "authorAvatar", null, "public");
        return immutable(resources);
    }

    public static Map<String, TsImageResourceVo> buildRoleImageProfileResources(Long ownerCharacterId,
                                                                                String selectedImageUrl,
                                                                                String sourceField,
                                                                                String ownerUserId) {
        Map<String, TsImageResourceVo> resources = new LinkedHashMap<>();
        putCharacterImage(resources, selectedImageUrl, sourceField, ownerCharacterId, null, ownerUserId);
        return immutable(resources);
    }

    public static Map<String, TsImageResourceVo> buildRoleImageProfilePublicResources(String selectedImageUrl,
                                                                                      String authorAvatar) {
        Map<String, TsImageResourceVo> resources = new LinkedHashMap<>();
        putCharacterImage(resources, selectedImageUrl, "selectedImageUrl", null, null, null);
        putUserAvatar(resources, authorAvatar, "authorAvatar", null, "public");
        return immutable(resources);
    }

    public static void putUserAvatar(Map<String, TsImageResourceVo> resources,
                                     String url,
                                     String sourceField,
                                     String userId,
                                     String privacy) {
        put(resources, IMAGE_TYPE_USER_AVATAR, url, sourceField, "medium", privacy, userId, null, null, null, null);
    }

    public static void putCharacterImage(Map<String, TsImageResourceVo> resources,
                                         String url,
                                         String sourceField,
                                         Long characterId,
                                         Long sourceImageId,
                                         String userId) {
        put(resources, IMAGE_TYPE_CHARACTER_IMAGE, url, sourceField, "medium", null, userId, characterId, null, null, sourceImageId);
    }

    public static void putCharacterAvatar(Map<String, TsImageResourceVo> resources,
                                          String url,
                                          String sourceField,
                                          Long characterId,
                                          Long sourceImageId,
                                          String userId) {
        put(resources, IMAGE_TYPE_CHARACTER_AVATAR, url, sourceField, "thumbnail", null, userId, characterId, null, null, sourceImageId);
    }

    public static void putStoryScene(Map<String, TsImageResourceVo> resources,
                                     String url,
                                     String sourceField,
                                     Long storyId,
                                     Long sceneId,
                                     String userId) {
        put(resources, IMAGE_TYPE_STORY_SCENE, url, sourceField, "medium", null, userId, null, storyId, sceneId, null);
    }

    private static void put(Map<String, TsImageResourceVo> resources,
                            String imageType,
                            String url,
                            String sourceField,
                            String variant,
                            String privacy,
                            String userId,
                            Long characterId,
                            Long storyId,
                            Long sceneId,
                            Long sourceImageId) {
        if (resources == null || !StringUtils.hasText(url)) {
            return;
        }
        TsImageResourceVo item = new TsImageResourceVo();
        item.setImageType(imageType);
        item.setUrl(url.trim());
        item.setSourceField(sourceField);
        item.setVariant(variant);
        item.setPrivacy(privacy);
        item.setUserId(trimToNull(userId));
        item.setCharacterId(characterId);
        item.setStoryId(storyId);
        item.setSceneId(sceneId);
        item.setSourceImageId(sourceImageId);
        resources.put(imageType, item);
    }

    private static Map<String, TsImageResourceVo> immutable(Map<String, TsImageResourceVo> resources) {
        if (resources == null || resources.isEmpty()) {
            return Collections.emptyMap();
        }
        return resources;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
