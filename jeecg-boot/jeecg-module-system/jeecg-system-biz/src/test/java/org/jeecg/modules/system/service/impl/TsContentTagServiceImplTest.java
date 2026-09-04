package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jeecg.modules.system.dto.tscontenttag.TsContentTagCandidateDto;
import org.jeecg.modules.system.entity.TsContentTag;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.entity.TsTag;
import org.jeecg.modules.system.entity.TsTagType;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.jeecg.modules.system.mapper.TsTagMapper;
import org.jeecg.modules.system.mapper.TsTagTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TsContentTagServiceImplTest {
    private TsContentTagServiceImpl service;
    private TsTagMapper tagMapper;
    private TsTagTypeMapper tagTypeMapper;
    private TsRoleMapper roleMapper;

    @BeforeEach
    void setUp() {
        service = spy(new TsContentTagServiceImpl());
        tagMapper = mock(TsTagMapper.class);
        tagTypeMapper = mock(TsTagTypeMapper.class);
        roleMapper = mock(TsRoleMapper.class);
        ReflectionTestUtils.setField(service, "tsTagMapper", tagMapper);
        ReflectionTestUtils.setField(service, "tsTagTypeMapper", tagTypeMapper);
        ReflectionTestUtils.setField(service, "tsRoleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "tsStoryMapper", mock(TsStoryMapper.class));
        doReturn(true).when(service).remove(any());
        doReturn(true).when(service).saveBatch(any());
    }

    @Test
    void replaceTagsKeepsOnlyFixedHighScoreTopThreePerType() {
        TsRole role = new TsRole();
        role.setId(10L);
        role.setContentVersion(1);
        when(roleMapper.selectById(10L)).thenReturn(role);
        when(tagMapper.selectList(any())).thenReturn(List.of(
                tag(1L, "personality", "温柔"),
                tag(2L, "personality", "成熟"),
                tag(3L, "personality", "活泼"),
                tag(4L, "personality", "冷淡")
        ));

        List<TsContentTagCandidateDto> candidates = new ArrayList<>();
        candidates.add(candidate("personality", "温柔", "0.91"));
        candidates.add(candidate("personality", "成熟", "0.72"));
        candidates.add(candidate("personality", "活泼", "0.82"));
        candidates.add(candidate("personality", "冷淡", "0.61"));
        candidates.add(candidate("personality", "不存在", "0.99"));
        candidates.add(candidate("personality", "温柔", "0.40"));

        int saved = service.replaceTags(
                "role", 10L, 1, "hash", "generation", "role_generate_role:v2",
                candidates, false);

        assertEquals(3, saved);
        ArgumentCaptor<List<TsContentTag>> captor = ArgumentCaptor.forClass(List.class);
        verify(service).saveBatch(captor.capture());
        List<TsContentTag> rows = captor.getValue();
        assertEquals(List.of(
                        new BigDecimal("0.9100"),
                        new BigDecimal("0.8200"),
                        new BigDecimal("0.7200")),
                rows.stream().map(TsContentTag::getScore).sorted(java.util.Comparator.reverseOrder()).toList());
    }

    @Test
    void replaceTagsRejectsStaleContentVersion() {
        TsRole role = new TsRole();
        role.setId(10L);
        role.setContentVersion(3);
        when(roleMapper.selectById(10L)).thenReturn(role);

        int saved = service.replaceTags(
                "role", 10L, 2, null, "ai_fallback", "ts_content_tagging:v1",
                List.of(candidate("personality", "温柔", "0.9")), false);

        assertEquals(0, saved);
        verify(tagMapper, never()).selectList(any());
        verify(service, never()).saveBatch(any());
    }

    @Test
    void findCurrentDisplayTagsReturnsOnlyRequestedVersionWithoutInternalScore() {
        TsContentTag stale = contentTag(10L, 1, 1L);
        TsContentTag current = contentTag(10L, 2, 2L);
        doReturn(List.of(stale, current)).when(service).list(any(Wrapper.class));

        TsTag tag = tag(2L, "personality", "成熟");
        tag.setSortOrder(2);
        when(tagMapper.selectList(any())).thenReturn(List.of(tag));

        TsTagType type = new TsTagType();
        type.setId("personality");
        type.setName("性格");
        type.setScope("role");
        type.setEnabled(1);
        type.setSortOrder(1);
        when(tagTypeMapper.selectList(any())).thenReturn(List.of(type));

        Map<Long, Integer> versions = new LinkedHashMap<>();
        versions.put(10L, 2);
        var result = service.findCurrentDisplayTags("role", versions);

        assertEquals(1, result.get(10L).size());
        assertEquals(2L, result.get(10L).get(0).getTagId());
        assertEquals("性格", result.get(10L).get(0).getTypeName());
        assertEquals("成熟", result.get(10L).get(0).getName());
    }

    private TsTag tag(Long id, String typeCode, String name) {
        TsTag tag = new TsTag();
        tag.setId(id);
        tag.setScope("role");
        tag.setTypeId(typeCode);
        tag.setName(name);
        tag.setEnabled(1);
        return tag;
    }

    private TsContentTagCandidateDto candidate(String typeCode, String name, String score) {
        TsContentTagCandidateDto candidate = new TsContentTagCandidateDto();
        candidate.setTypeCode(typeCode);
        candidate.setName(name);
        candidate.setScore(new BigDecimal(score));
        return candidate;
    }

    private TsContentTag contentTag(Long contentId, Integer contentVersion, Long tagId) {
        TsContentTag row = new TsContentTag();
        row.setContentType("role");
        row.setContentId(contentId);
        row.setContentVersion(contentVersion);
        row.setTagId(tagId);
        row.setScore(new BigDecimal("0.9000"));
        return row;
    }
}
