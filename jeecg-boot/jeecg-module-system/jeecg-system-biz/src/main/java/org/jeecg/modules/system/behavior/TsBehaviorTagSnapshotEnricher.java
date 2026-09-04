package org.jeecg.modules.system.behavior;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.entity.TsContentTag;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.jeecg.modules.system.mapper.TsContentTagMapper;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 为角色和故事行为补充服务端可信标签快照。 */
@Slf4j
@Component
public class TsBehaviorTagSnapshotEnricher {
    private static final int EVENT_VERSION = 3;

    private final TsRoleMapper roleMapper;
    private final TsStoryMapper storyMapper;
    private final TsContentTagMapper contentTagMapper;

    /** 注入角色、故事和内容标签 Mapper。 */
    public TsBehaviorTagSnapshotEnricher(
            TsRoleMapper roleMapper,
            TsStoryMapper storyMapper,
            TsContentTagMapper contentTagMapper) {
        this.roleMapper = roleMapper;
        this.storyMapper = storyMapper;
        this.contentTagMapper = contentTagMapper;
    }

    /**
     * 按资源去重并批量补充当前内容版本标签；查询失败时保留空快照。
     */
    public void enrich(List<TsBehaviorEventMessage> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        initializeSnapshots(events);
        try {
            enrichSafely(events);
        } catch (RuntimeException exception) {
            log.warn("行为事件标签快照补充失败，将使用空标签快照", exception);
        }
    }

    /** 初始化 v3 事件和空标签数组，保证无标签内容也可稳定写入 ClickHouse。 */
    private void initializeSnapshots(List<TsBehaviorEventMessage> events) {
        for (TsBehaviorEventMessage event : events) {
            if (event == null) {
                continue;
            }
            event.setEventVersion(EVENT_VERSION);
            event.setContentVersion(null);
            event.setTagIds(List.of());
            event.setTagScores(List.of());
        }
    }

    /** 执行资源版本和对应标签的批量查询与映射。 */
    private void enrichSafely(List<TsBehaviorEventMessage> events) {
        Map<String, ResourceRef> resources = collectResources(events);
        if (resources.isEmpty()) {
            return;
        }
        Map<String, Integer> versions = loadVersions(resources.values());
        Map<String, List<TsContentTag>> tagsByResource =
                loadCurrentTags(resources.values(), versions);
        applySnapshots(events, versions, tagsByResource);
    }

    /** 收集可关联固定标签的角色和故事资源。 */
    private Map<String, ResourceRef> collectResources(List<TsBehaviorEventMessage> events) {
        Map<String, ResourceRef> result = new LinkedHashMap<>();
        for (TsBehaviorEventMessage event : events) {
            if (event == null || !isContentType(event.getResourceType())) {
                continue;
            }
            Long contentId = parseContentId(event.getResourceId());
            if (contentId == null) {
                continue;
            }
            ResourceRef ref = new ResourceRef(event.getResourceType(), contentId);
            result.putIfAbsent(ref.key(), ref);
        }
        return result;
    }

    /** 批量加载角色和故事的当前内容版本。 */
    private Map<String, Integer> loadVersions(Iterable<ResourceRef> resources) {
        Set<Long> roleIds = new LinkedHashSet<>();
        Set<Long> storyIds = new LinkedHashSet<>();
        for (ResourceRef resource : resources) {
            if ("role".equals(resource.contentType())) {
                roleIds.add(resource.contentId());
            } else {
                storyIds.add(resource.contentId());
            }
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!roleIds.isEmpty()) {
            QueryWrapper<TsRole> wrapper = new QueryWrapper<>();
            wrapper.in("id", roleIds).select("id", "content_version");
            for (TsRole role : roleMapper.selectList(wrapper)) {
                result.put(resourceKey("role", role.getId()), role.getContentVersion());
            }
        }
        if (!storyIds.isEmpty()) {
            QueryWrapper<TsStory> wrapper = new QueryWrapper<>();
            wrapper.in("id", storyIds).select("id", "content_version");
            for (TsStory story : storyMapper.selectList(wrapper)) {
                result.put(resourceKey("story", story.getId()), story.getContentVersion());
            }
        }
        return result;
    }

    /** 按内容类型批量读取与当前版本一致的标签。 */
    private Map<String, List<TsContentTag>> loadCurrentTags(
            Iterable<ResourceRef> resources,
            Map<String, Integer> versions) {
        Set<Long> roleIds = new LinkedHashSet<>();
        Set<Long> storyIds = new LinkedHashSet<>();
        for (ResourceRef resource : resources) {
            if (!versions.containsKey(resource.key())) {
                continue;
            }
            if ("role".equals(resource.contentType())) {
                roleIds.add(resource.contentId());
            } else {
                storyIds.add(resource.contentId());
            }
        }
        Map<String, List<TsContentTag>> result = new LinkedHashMap<>();
        appendTags(result, "role", roleIds, versions);
        appendTags(result, "story", storyIds, versions);
        return result;
    }

    /** 查询单一内容类型的标签并过滤旧版本记录。 */
    private void appendTags(
            Map<String, List<TsContentTag>> target,
            String contentType,
            Set<Long> contentIds,
            Map<String, Integer> versions) {
        if (contentIds.isEmpty()) {
            return;
        }
        QueryWrapper<TsContentTag> wrapper = new QueryWrapper<>();
        wrapper.eq("content_type", contentType)
                .in("content_id", contentIds)
                .orderByAsc("tag_id");
        for (TsContentTag tag : contentTagMapper.selectList(wrapper)) {
            String key = resourceKey(contentType, tag.getContentId());
            Integer currentVersion = versions.get(key);
            if (currentVersion != null && currentVersion.equals(tag.getContentVersion())) {
                target.computeIfAbsent(key, ignored -> new ArrayList<>()).add(tag);
            }
        }
    }

    /** 将内容版本、标签 ID 和分数写回对应事件。 */
    private void applySnapshots(
            List<TsBehaviorEventMessage> events,
            Map<String, Integer> versions,
            Map<String, List<TsContentTag>> tagsByResource) {
        for (TsBehaviorEventMessage event : events) {
            if (event == null || !isContentType(event.getResourceType())) {
                continue;
            }
            Long contentId = parseContentId(event.getResourceId());
            if (contentId == null) {
                continue;
            }
            String key = resourceKey(event.getResourceType(), contentId);
            event.setContentVersion(versions.get(key));
            List<TsContentTag> tags = new ArrayList<>(
                    tagsByResource.getOrDefault(key, List.of()));
            tags.sort(Comparator.comparing(TsContentTag::getTagId));
            event.setTagIds(tags.stream().map(TsContentTag::getTagId).toList());
            event.setTagScores(tags.stream()
                    .map(TsContentTag::getScore)
                    .map(score -> score == null ? BigDecimal.ZERO : score)
                    .toList());
        }
    }

    /** 判断资源是否属于角色或故事内容。 */
    private boolean isContentType(String resourceType) {
        return "role".equals(resourceType) || "story".equals(resourceType);
    }

    /** 将资源 ID 安全转换为数据库主键。 */
    private Long parseContentId(String resourceId) {
        if (!StringUtils.hasText(resourceId)) {
            return null;
        }
        try {
            return Long.valueOf(resourceId.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** 生成内容类型和资源 ID 的稳定关联键。 */
    private String resourceKey(String contentType, Long contentId) {
        return contentType + "\u0000" + contentId;
    }

    /** 行为事件对应的角色或故事引用。 */
    private record ResourceRef(String contentType, Long contentId) {
        /** 返回稳定关联键。 */
        private String key() {
            return contentType + "\u0000" + contentId;
        }
    }
}
