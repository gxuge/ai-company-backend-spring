package org.jeecg.modules.system.behavior;

import org.jeecg.modules.system.entity.TsContentTag;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.jeecg.modules.system.mapper.TsContentTagMapper;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 行为事件标签快照补全测试。 */
class TsBehaviorTagSnapshotEnricherTest {

    /** 只应保存当前内容版本的标签，并按标签 ID 稳定排序。 */
    @Test
    void enrichShouldUseCurrentContentVersionTags() {
        TsRoleMapper roleMapper = mock(TsRoleMapper.class);
        TsStoryMapper storyMapper = mock(TsStoryMapper.class);
        TsContentTagMapper contentTagMapper = mock(TsContentTagMapper.class);
        TsBehaviorTagSnapshotEnricher enricher =
                new TsBehaviorTagSnapshotEnricher(
                        roleMapper, storyMapper, contentTagMapper);

        TsRole role = new TsRole();
        role.setId(1L);
        role.setContentVersion(2);
        when(roleMapper.selectList(any())).thenReturn(List.of(role));

        TsContentTag currentHigh = contentTag(1L, 2, 12L, "0.9000");
        TsContentTag currentLow = contentTag(1L, 2, 11L, "0.7000");
        TsContentTag obsolete = contentTag(1L, 1, 10L, "0.9900");
        when(contentTagMapper.selectList(any()))
                .thenReturn(List.of(currentHigh, obsolete, currentLow));

        TsBehaviorEventMessage event = new TsBehaviorEventMessage()
                .setEventVersion(2)
                .setResourceType("role")
                .setResourceId("1");

        enricher.enrich(List.of(event));

        assertEquals(3, event.getEventVersion());
        assertEquals(2, event.getContentVersion());
        assertEquals(List.of(11L, 12L), event.getTagIds());
        assertEquals(
                List.of(new BigDecimal("0.7000"), new BigDecimal("0.9000")),
                event.getTagScores());
    }

    /** 构造内容标签测试数据。 */
    private TsContentTag contentTag(
            Long contentId,
            Integer contentVersion,
            Long tagId,
            String score) {
        TsContentTag tag = new TsContentTag();
        tag.setContentType("role");
        tag.setContentId(contentId);
        tag.setContentVersion(contentVersion);
        tag.setTagId(tagId);
        tag.setScore(new BigDecimal(score));
        return tag;
    }
}
