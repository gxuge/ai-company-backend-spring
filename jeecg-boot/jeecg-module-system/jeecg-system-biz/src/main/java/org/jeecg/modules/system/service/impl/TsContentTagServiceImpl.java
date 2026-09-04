package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.modules.system.dto.tscontenttag.TsContentTagCandidateDto;
import org.jeecg.modules.system.entity.TsContentTag;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.entity.TsTag;
import org.jeecg.modules.system.entity.TsTagType;
import org.jeecg.modules.system.mapper.TsContentTagMapper;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.jeecg.modules.system.mapper.TsTagMapper;
import org.jeecg.modules.system.mapper.TsTagTypeMapper;
import org.jeecg.modules.system.service.ITsContentTagService;
import org.jeecg.modules.system.vo.tscontenttag.TsContentTagDisplayVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 内容标签校验和版本化存储实现。 */
@Service
public class TsContentTagServiceImpl extends ServiceImpl<TsContentTagMapper, TsContentTag>
        implements ITsContentTagService {

    private static final BigDecimal MIN_SCORE = new BigDecimal("0.5");
    private static final BigDecimal MAX_SCORE = BigDecimal.ONE;
    private static final int MAX_TAGS_PER_TYPE = 3;

    @Resource
    private TsTagMapper tsTagMapper;
    @Resource
    private TsTagTypeMapper tsTagTypeMapper;
    @Resource
    private TsRoleMapper tsRoleMapper;
    @Resource
    private TsStoryMapper tsStoryMapper;

    /** 从模型返回的数组中兼容读取蛇形和驼峰字段。 */
    @Override
    public List<TsContentTagCandidateDto> parseCandidates(Object rawTags) {
        JSONArray array = toArray(rawTags);
        List<TsContentTagCandidateDto> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (Object item : array) {
            JSONObject json = item instanceof JSONObject
                    ? (JSONObject) item : JSONObject.parseObject(JSONObject.toJSONString(item));
            if (json == null) {
                continue;
            }
            TsContentTagCandidateDto candidate = new TsContentTagCandidateDto();
            candidate.setTypeCode(firstText(json.getString("type_code"), json.getString("typeCode")));
            candidate.setName(trimToNull(json.getString("name")));
            candidate.setScore(json.getBigDecimal("score"));
            result.add(candidate);
        }
        return result;
    }

    /** 校验固定词典、分数和版本后，以当前内容为单位覆盖保存标签。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int replaceTags(String contentType, Long contentId, Integer contentVersion, String contentHash,
                           String source, String modelVersion, List<TsContentTagCandidateDto> candidates,
                           boolean allowNextVersion) {
        String normalizedType = normalizeContentType(contentType);
        if (contentId == null || contentVersion == null || contentVersion < 1) {
            return 0;
        }
        Integer actualVersion = findCurrentVersion(normalizedType, contentId);
        boolean versionMatched = actualVersion != null && actualVersion.equals(contentVersion);
        boolean nextVersionMatched = allowNextVersion && actualVersion != null
                && actualVersion + 1 == contentVersion;
        if (!versionMatched && !nextVersionMatched) {
            return 0;
        }
        List<ResolvedTag> resolvedTags = resolveCandidates(normalizedType, candidates);
        if (resolvedTags.isEmpty()) {
            return 0;
        }

        LambdaQueryWrapper<TsContentTag> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(TsContentTag::getContentType, normalizedType)
                .eq(TsContentTag::getContentId, contentId);
        this.remove(deleteWrapper);

        Date now = new Date();
        List<TsContentTag> rows = new ArrayList<>(resolvedTags.size());
        for (ResolvedTag resolved : resolvedTags) {
            TsContentTag row = new TsContentTag();
            row.setContentType(normalizedType);
            row.setContentId(contentId);
            row.setContentVersion(contentVersion);
            row.setTagId(resolved.tag().getId());
            row.setScore(resolved.score().setScale(4, RoundingMode.HALF_UP));
            row.setSource(trimToNull(source) == null ? "generation" : source.trim());
            row.setModelVersion(trimToNull(modelVersion));
            row.setContentHash(trimToNull(contentHash));
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            rows.add(row);
        }
        this.saveBatch(rows);
        return rows.size();
    }

    /** 判断当前内容版本是否已有至少一个标签。 */
    @Override
    public boolean hasTags(String contentType, Long contentId, Integer contentVersion) {
        if (contentId == null || contentVersion == null) {
            return false;
        }
        LambdaQueryWrapper<TsContentTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TsContentTag::getContentType, normalizeContentType(contentType))
                .eq(TsContentTag::getContentId, contentId)
                .eq(TsContentTag::getContentVersion, contentVersion);
        return this.count(wrapper) > 0;
    }

    /** 按类型输出供 AI 使用的精简固定词典。 */
    @Override
    public String buildDictionaryJson(String contentType) {
        String normalizedType = normalizeContentType(contentType);
        LambdaQueryWrapper<TsTagType> typeWrapper = new LambdaQueryWrapper<>();
        typeWrapper.eq(TsTagType::getScope, normalizedType)
                .eq(TsTagType::getEnabled, 1)
                .orderByAsc(TsTagType::getSortOrder);
        List<TsTagType> types = tsTagTypeMapper.selectList(typeWrapper);

        LambdaQueryWrapper<TsTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(TsTag::getScope, normalizedType)
                .eq(TsTag::getEnabled, 1)
                .orderByAsc(TsTag::getSortOrder);
        List<TsTag> tags = tsTagMapper.selectList(tagWrapper);
        Map<String, JSONArray> namesByType = new LinkedHashMap<>();
        for (TsTagType type : types) {
            namesByType.put(type.getId(), new JSONArray());
        }
        for (TsTag tag : tags) {
            JSONArray names = namesByType.get(tag.getTypeId());
            if (names != null) {
                names.add(tag.getName());
            }
        }
        JSONArray dictionary = new JSONArray();
        for (TsTagType type : types) {
            JSONObject item = new JSONObject();
            item.put("type_code", type.getId());
            item.put("type_name", type.getName());
            item.put("tags", namesByType.get(type.getId()));
            dictionary.add(item);
        }
        return dictionary.toJSONString();
    }

    /** 批量读取内容当前版本标签，并映射为不含内部评分信息的展示结构。 */
    @Override
    public Map<Long, List<TsContentTagDisplayVo>> findCurrentDisplayTags(
            String contentType, Map<Long, Integer> contentVersions) {
        String normalizedType = normalizeContentType(contentType);
        Map<Long, Integer> validVersions = new LinkedHashMap<>();
        if (contentVersions != null) {
            for (Map.Entry<Long, Integer> entry : contentVersions.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    validVersions.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (validVersions.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<TsContentTagDisplayVo>> result = new LinkedHashMap<>();
        for (Long contentId : validVersions.keySet()) {
            result.put(contentId, new ArrayList<>());
        }

        LambdaQueryWrapper<TsContentTag> contentTagWrapper = new LambdaQueryWrapper<>();
        contentTagWrapper.eq(TsContentTag::getContentType, normalizedType)
                .in(TsContentTag::getContentId, validVersions.keySet());
        List<TsContentTag> contentTags = this.list(contentTagWrapper);
        List<TsContentTag> currentRows = new ArrayList<>();
        Set<Long> tagIds = new HashSet<>();
        for (TsContentTag row : contentTags) {
            if (row == null || row.getContentId() == null || row.getTagId() == null
                    || row.getContentVersion() == null
                    || !row.getContentVersion().equals(validVersions.get(row.getContentId()))) {
                continue;
            }
            currentRows.add(row);
            tagIds.add(row.getTagId());
        }
        if (tagIds.isEmpty()) {
            return result;
        }

        LambdaQueryWrapper<TsTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(TsTag::getScope, normalizedType)
                .eq(TsTag::getEnabled, 1)
                .in(TsTag::getId, tagIds);
        List<TsTag> tags = tsTagMapper.selectList(tagWrapper);
        Map<Long, TsTag> tagMap = new HashMap<>();
        Set<String> typeIds = new HashSet<>();
        for (TsTag tag : tags) {
            if (tag == null || tag.getId() == null || tag.getTypeId() == null) {
                continue;
            }
            tagMap.put(tag.getId(), tag);
            typeIds.add(tag.getTypeId());
        }
        if (tagMap.isEmpty()) {
            return result;
        }
        currentRows.removeIf(row -> !tagMap.containsKey(row.getTagId()));

        LambdaQueryWrapper<TsTagType> typeWrapper = new LambdaQueryWrapper<>();
        typeWrapper.eq(TsTagType::getScope, normalizedType)
                .eq(TsTagType::getEnabled, 1)
                .in(TsTagType::getId, typeIds);
        List<TsTagType> types = tsTagTypeMapper.selectList(typeWrapper);
        Map<String, TsTagType> typeMap = new HashMap<>();
        for (TsTagType type : types) {
            if (type != null && type.getId() != null) {
                typeMap.put(type.getId(), type);
            }
        }

        currentRows.sort(Comparator
                .comparingInt((TsContentTag row) ->
                        sortOrder(typeMap.get(tagMap.get(row.getTagId()).getTypeId())))
                .thenComparingInt(row -> sortOrder(tagMap.get(row.getTagId())))
                .thenComparing(row -> tagMap.get(row.getTagId()).getName(),
                        Comparator.nullsLast(String::compareTo)));
        for (TsContentTag row : currentRows) {
            TsTag tag = tagMap.get(row.getTagId());
            if (tag == null) {
                continue;
            }
            TsTagType type = typeMap.get(tag.getTypeId());
            if (type == null) {
                continue;
            }
            TsContentTagDisplayVo item = new TsContentTagDisplayVo();
            item.setTagId(tag.getId());
            item.setTypeCode(type.getId());
            item.setTypeName(type.getName());
            item.setName(tag.getName());
            result.get(row.getContentId()).add(item);
        }
        return result;
    }

    private List<ResolvedTag> resolveCandidates(String contentType, List<TsContentTagCandidateDto> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<TsTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TsTag::getScope, contentType).eq(TsTag::getEnabled, 1);
        List<TsTag> dictionary = tsTagMapper.selectList(wrapper);
        Map<String, TsTag> dictionaryMap = new HashMap<>();
        for (TsTag tag : dictionary) {
            dictionaryMap.put(dictionaryKey(tag.getTypeId(), tag.getName()), tag);
        }

        Map<Long, ResolvedTag> highestByTag = new HashMap<>();
        for (TsContentTagCandidateDto candidate : candidates) {
            if (candidate == null || candidate.getScore() == null
                    || candidate.getScore().compareTo(MIN_SCORE) < 0
                    || candidate.getScore().compareTo(MAX_SCORE) > 0) {
                continue;
            }
            TsTag tag = dictionaryMap.get(dictionaryKey(candidate.getTypeCode(), candidate.getName()));
            if (tag == null) {
                continue;
            }
            ResolvedTag current = highestByTag.get(tag.getId());
            if (current == null || candidate.getScore().compareTo(current.score()) > 0) {
                highestByTag.put(tag.getId(), new ResolvedTag(tag, candidate.getScore()));
            }
        }

        Map<String, List<ResolvedTag>> grouped = new LinkedHashMap<>();
        for (ResolvedTag item : highestByTag.values()) {
            grouped.computeIfAbsent(item.tag().getTypeId(), key -> new ArrayList<>()).add(item);
        }
        List<ResolvedTag> result = new ArrayList<>();
        for (List<ResolvedTag> group : grouped.values()) {
            group.sort(Comparator.comparing(ResolvedTag::score).reversed());
            result.addAll(group.subList(0, Math.min(MAX_TAGS_PER_TYPE, group.size())));
        }
        return result;
    }

    private Integer findCurrentVersion(String contentType, Long contentId) {
        if ("role".equals(contentType)) {
            TsRole role = tsRoleMapper.selectById(contentId);
            return role == null ? null : role.getContentVersion();
        }
        TsStory story = tsStoryMapper.selectById(contentId);
        return story == null ? null : story.getContentVersion();
    }

    private JSONArray toArray(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            return JSONArray.parseArray((String) value);
        }
        return JSONArray.parseArray(JSONObject.toJSONString(value));
    }

    private String normalizeContentType(String value) {
        String normalized = trimToNull(value);
        if ("character".equals(normalized)) {
            return "role";
        }
        if (!"role".equals(normalized) && !"story".equals(normalized)) {
            throw new IllegalArgumentException("contentType 仅支持 role 或 story");
        }
        return normalized;
    }

    private String dictionaryKey(String typeCode, String name) {
        return String.valueOf(trimToNull(typeCode)) + "\u0000" + String.valueOf(trimToNull(name));
    }

    private String firstText(String first, String second) {
        String normalized = trimToNull(first);
        return normalized == null ? trimToNull(second) : normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int sortOrder(TsTagType value) {
        return value == null || value.getSortOrder() == null ? Integer.MAX_VALUE : value.getSortOrder();
    }

    private int sortOrder(TsTag value) {
        return value == null || value.getSortOrder() == null ? Integer.MAX_VALUE : value.getSortOrder();
    }

    private record ResolvedTag(TsTag tag, BigDecimal score) {
    }
}
