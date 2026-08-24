package org.jeecg.modules.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.behavior.TsBehaviorEventPublisher;
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

/** 推荐行为采集服务测试。 */
class TsBehaviorEventServiceImplTest {
    private TsBehaviorEventPublisher publisher;
    private TsBehaviorConfigBean config;
    private TsBehaviorEventServiceImpl service;

    /** 初始化采集服务依赖。 */
    @BeforeEach
    void setUp() {
        publisher = mock(TsBehaviorEventPublisher.class);
        config = new TsBehaviorConfigBean();
        service = new TsBehaviorEventServiceImpl(
                publisher, config, new ObjectMapper());
    }

    /** 用户ID必须由登录态覆盖并按事件数量逐条提交。 */
    @Test
    void collectShouldPublishTrustedUserEvent() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        TsBehaviorEventDto request = validRequest();
        request.setProperties(Map.of("tag", "fantasy"));

        assertEquals(1, service.collect(user, List.of(request)).getAcceptedCount());

        ArgumentCaptor<TsBehaviorEventMessage> captor =
                ArgumentCaptor.forClass(TsBehaviorEventMessage.class);
        verify(publisher).publish(captor.capture());
        assertEquals("u1", captor.getValue().getUserId());
        assertEquals("WEB", captor.getValue().getPlatform());
        assertEquals("{\"tag\":\"fantasy\"}", captor.getValue().getPropertiesJson());
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

    /** 构建合法行为请求。 */
    private TsBehaviorEventDto validRequest() {
        TsBehaviorEventDto request = new TsBehaviorEventDto();
        request.setEventId("event-1");
        request.setEventType("click");
        request.setSessionId("session-1");
        request.setResourceType("story");
        request.setResourceId("1");
        return request;
    }
}
