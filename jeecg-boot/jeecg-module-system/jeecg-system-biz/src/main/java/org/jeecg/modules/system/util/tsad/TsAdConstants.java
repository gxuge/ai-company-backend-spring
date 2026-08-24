package org.jeecg.modules.system.util.tsad;

import java.util.Set;

/** 广告投放域固定取值。 */
public final class TsAdConstants {
    public static final Set<String> SLOT_TYPES =
            Set.of("BANNER", "POSTER", "POPUP", "CAROUSEL");
    public static final Set<String> SLOT_STATUSES = Set.of("ENABLED", "DISABLED");
    public static final Set<String> CONTENT_STATUSES =
            Set.of("DRAFT", "PUBLISHED", "OFFLINE");
    public static final Set<String> SOURCE_TYPES =
            Set.of("SELF", "EXTERNAL", "AD_NETWORK");
    public static final Set<String> MEDIA_TYPES =
            Set.of("IMAGE", "VIDEO", "CARD");
    public static final Set<String> CARD_TYPES =
            Set.of("PROMOTION", "ROLE", "STORY", "CUSTOM");
    public static final Set<String> LINK_TYPES =
            Set.of("NONE", "URL", "ROUTE", "ROLE", "STORY", "DEEP_LINK");
    public static final Set<String> ACTION_TYPES = LINK_TYPES;
    public static final Set<String> PLATFORMS = Set.of("ALL", "WEB", "IOS", "ANDROID");
    public static final Set<String> AUDIENCE_TYPES =
            Set.of("ALL", "LOGIN", "ANONYMOUS", "USER_LIST");
    public static final Set<String> MEMBER_LEVELS =
            Set.of("ALL", "FREE", "PRO", "ULTRA");
    public static final Set<String> EVENT_TYPES = Set.of("IMPRESSION", "CLICK");

    /** 工具类不允许实例化。 */
    private TsAdConstants() {
    }
}
