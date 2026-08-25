package org.jeecg.modules.system.vo.tschatsession;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsChatSession;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public final class TsChatSessionVoConverter {
    private static final String DEFAULT_SYSTEM_SESSION_KEY = "DEFAULT_SYSTEM";

    private TsChatSessionVoConverter() {
    }
    public static Page<TsChatSessionVo> fromPage(Page<TsChatSession> source) {
        return fromPage(source, null);
    }

    public static Page<TsChatSessionVo> fromPage(
        Page<TsChatSession> source,
        Map<Long, TsChatSessionSummaryVo> summaryMap) {
        Page<TsChatSessionVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsChatSessionVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsChatSession item : source.getRecords()) {
                TsChatSessionSummaryVo summary = summaryMap == null ? null : summaryMap.get(item.getId());
                records.add(fromEntityWithSummary(item, summary));
            }
        }
        target.setRecords(records);
        return target;
    }

    /**
     * 将会话实体转换为带列表摘要的响应。
     *
     * @param source 会话实体
     * @param summary 列表摘要
     * @return 会话响应
     */
    public static TsChatSessionVo fromEntityWithSummary(
        TsChatSession source,
        TsChatSessionSummaryVo summary) {
        TsChatSessionVo target = fromEntity(
            source,
            summary == null ? null : summary.getRoleAvatarUrl());
        if (target == null || summary == null) {
            return target;
        }
        target.setRoleName(summary.getRoleName());
        target.setLastMessageText(summary.getLastMessageText());
        return target;
    }

    public static TsChatSessionVo fromEntity(TsChatSession source) {
        return fromEntity(source, null);
    }

    public static TsChatSessionVo fromEntity(TsChatSession source, String roleAvatarUrl) {
        if (source == null) {
            return null;
        }
        TsChatSessionVo target = new TsChatSessionVo();
        target.setId(source.getId());
        target.setUserId(source.getUserId());
        target.setSessionType(source.getSessionType());
        target.setIsSystemSession(DEFAULT_SYSTEM_SESSION_KEY.equals(source.getSystemSessionKey()));
        target.setSessionTitle(source.getSessionTitle());
        target.setTargetRoleId(source.getTargetRoleId());
        target.setRoleAvatarUrl(roleAvatarUrl);
        target.setStoryId(source.getStoryId());
        target.setSessionStatus(source.getSessionStatus());
        target.setLastMessageId(source.getLastMessageId());
        target.setLastMessageAt(source.getLastMessageAt());
        target.setExtJson(source.getExtJson());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setImageResources(TsImageResourceResolver.buildChatSessionImageResources(
                source.getTargetRoleId(),
                roleAvatarUrl));
        return target;
    }
}
