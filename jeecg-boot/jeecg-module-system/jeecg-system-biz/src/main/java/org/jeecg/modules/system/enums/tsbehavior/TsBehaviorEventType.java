package org.jeecg.modules.system.enums.tsbehavior;

import java.util.Arrays;
import java.util.Optional;

/**
 * 可采集的业务行为事件。
 */
public enum TsBehaviorEventType {
    USER_LANGUAGE("user_language"),
    DETAIL_VIEW("detail_view"),
    IMPRESSION("impression"),
    FAVORITE("favorite"),
    UNFAVORITE("unfavorite"),
    CONNECTION("connection"),
    CHAT_MESSAGE("chat_message"),
    ROLE_CREATE("role_create"),
    STORY_CREATE("story_create"),
    ROLE_IMAGE_GENERATE("role_image_generate"),
    STORY_BACKGROUND_GENERATE("story_background_generate");

    private final String code;

    TsBehaviorEventType(String code) {
        this.code = code;
    }

    /**
     * 返回事件编码。
     */
    public String getCode() {
        return code;
    }

    /**
     * 按编码查找事件类型。
     */
    public static Optional<TsBehaviorEventType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst();
    }
}
