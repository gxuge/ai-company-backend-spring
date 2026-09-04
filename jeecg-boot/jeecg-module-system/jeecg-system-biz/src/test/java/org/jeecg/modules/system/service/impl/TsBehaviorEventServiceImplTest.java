package org.jeecg.modules.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.behavior.TsBehaviorEventPublisher;
import org.jeecg.modules.system.behavior.TsBehaviorTagSnapshotEnricher;
import org.jeecg.modules.system.config.TsBehaviorConfigBean;
import org.jeecg.modules.system.dto.tsbehavior.TsBehaviorEventDto;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 业务行为采集服务测试。 */
class TsBehaviorEventServiceImplTest {
    private TsBehaviorEventPublisher publisher;
    private TsBehaviorTagSnapshotEnricher tagSnapshotEnricher;
    private TsBehaviorConfigBean config;
    private TsBehaviorEventServiceImpl service;

    /** 初始化采集服务依赖。 */
    @BeforeEach
    void setUp() {
        publisher = mock(TsBehaviorEventPublisher.class);
        tagSnapshotEnricher = mock(TsBehaviorTagSnapshotEnricher.class);
        config = new TsBehaviorConfigBean();
        config.getKafka().setEnabled(true);
        service = new TsBehaviorEventServiceImpl(
                publisher, tagSnapshotEnricher, config, new ObjectMapper());
    }

    /** 行为采集关闭时应返回零并且不提交Kafka。 */
    @Test
    void collectShouldSkipWhenKafkaIsDisabled() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        config.getKafka().setEnabled(false);

        assertEquals(
                0,
                service.collect(user, List.of(validRequest())).getAcceptedCount());
        verify(publisher, never()).publish(any());
    }

    /** 用户ID必须由登录态覆盖并按事件数量逐条提交。 */
    @Test
    void collectShouldPublishTrustedUserEvent() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto request = validRequest();
        request.setProperties(Map.of());

        assertEquals(1, service.collect(user, List.of(request)).getAcceptedCount());

        ArgumentCaptor<TsBehaviorEventMessage> captor =
                ArgumentCaptor.forClass(TsBehaviorEventMessage.class);
        verify(publisher).publish(captor.capture());
        assertEquals("u1", captor.getValue().getUserId());
        assertEquals("WEB", captor.getValue().getPlatform());
        assertEquals("detail_view", captor.getValue().getEventType());
        assertEquals(3, captor.getValue().getEventVersion());
        verify(tagSnapshotEnricher).enrich(any());
    }

    /** 超出允许时间窗口的事件必须拒绝。 */
    @Test
    void collectShouldRejectExpiredEvent() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto request = validRequest();
        request.setOccurredAt(new Date(
                System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000));

        assertThrows(
                JeecgBootException.class,
                () -> service.collect(user, List.of(request)));
    }

    /** 超出配置批量上限的事件必须拒绝。 */
    @Test
    void collectShouldRejectOversizedBatch() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        config.setMaxBatchSize(1);

        assertThrows(
                JeecgBootException.class,
                () -> service.collect(user, List.of(validRequest(), validRequest())));
    }

    /** 整批校验失败时不得提前投递前面的合法事件。 */
    @Test
    void collectShouldNotPublishPartialBatch() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto invalid = validRequest();
        invalid.setPlatform("UNKNOWN");

        assertThrows(
                JeecgBootException.class,
                () -> service.collect(user, List.of(validRequest(), invalid)));
        verify(publisher, never()).publish(any());
    }

    /** 旧事件类型必须被拒绝。 */
    @Test
    void collectShouldRejectLegacyEventType() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto request = validRequest();
        request.setEventType("click");

        assertThrows(
                JeecgBootException.class,
                () -> service.collect(user, List.of(request)));
    }

    /** 当前阶段不允许上传角色或故事标签。 */
    @Test
    void collectShouldRejectTagProperty() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto request = validRequest();
        request.setProperties(Map.of("tag", "fantasy"));

        assertThrows(
                JeecgBootException.class,
                () -> service.collect(user, List.of(request)));
    }

    /** 客户端不得伪造取消收藏事件。 */
    @Test
    void collectShouldRejectClientUnfavorite() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto request = validRequest();
        request.setEventType("unfavorite");

        assertThrows(
                JeecgBootException.class,
                () -> service.collect(user, List.of(request)));
        verify(publisher, never()).publish(any());
    }

    /** 合法推荐曝光必须携带场景、请求标识和正整数位置。 */
    @Test
    void collectShouldPublishRecommendationImpression() throws Exception {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto request = validImpressionRequest();

        assertEquals(1, service.collect(user, List.of(request)).getAcceptedCount());

        ArgumentCaptor<TsBehaviorEventMessage> captor =
                ArgumentCaptor.forClass(TsBehaviorEventMessage.class);
        verify(publisher).publish(captor.capture());
        TsBehaviorEventMessage message = captor.getValue();
        assertEquals("impression", message.getEventType());
        assertEquals(
                "browse_story",
                new ObjectMapper().readTree(message.getPropertiesJson())
                        .get("scene").asText());
        assertEquals(
                "request-1",
                new ObjectMapper().readTree(message.getPropertiesJson())
                        .get("requestId").asText());
        assertEquals(
                "3",
                new ObjectMapper().readTree(message.getPropertiesJson())
                        .get("position").asText());
    }

    /** 推荐曝光缺少归因字段时必须拒绝。 */
    @Test
    void collectShouldRejectIncompleteRecommendationImpression() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto request = validImpressionRequest();
        request.setProperties(Map.of(
                "scene", "browse_story",
                "requestId", "request-1"));

        assertThrows(
                JeecgBootException.class,
                () -> service.collect(user, List.of(request)));
        verify(publisher, never()).publish(any());
    }

    /** 推荐曝光位置不是正整数时必须拒绝。 */
    @Test
    void collectShouldRejectInvalidRecommendationPosition() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto request = validImpressionRequest();
        request.setProperties(Map.of(
                "scene", "browse_story",
                "requestId", "request-1",
                "position", "0"));

        assertThrows(
                JeecgBootException.class,
                () -> service.collect(user, List.of(request)));
        verify(publisher, never()).publish(any());
    }

    /** 构建合法行为请求。 */
    private TsBehaviorEventDto validRequest() {
        TsBehaviorEventDto request = new TsBehaviorEventDto();
        request.setEventId("event-1");
        request.setEventType("detail_view");
        request.setSessionId("session-1");
        request.setResourceType("story");
        request.setResourceId("1");
        return request;
    }

    /** 构建合法推荐曝光请求。 */
    private TsBehaviorEventDto validImpressionRequest() {
        TsBehaviorEventDto request = validRequest();
        request.setEventType("impression");
        request.setProperties(Map.of(
                "scene", "browse_story",
                "requestId", "request-1",
                "position", "3"));
        return request;
    }
}
