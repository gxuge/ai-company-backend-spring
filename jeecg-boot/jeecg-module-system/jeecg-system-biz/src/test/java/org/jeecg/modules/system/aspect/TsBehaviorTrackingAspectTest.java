package org.jeecg.modules.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.annotation.TsBehaviorTrack;
import org.jeecg.modules.system.behavior.TsBehaviorEventCoordinator;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.argThat;

class TsBehaviorTrackingAspectTest {

    private TsBehaviorEventCoordinator coordinator;
    private TrackedService service;

    @BeforeEach
    void setUp() {
        coordinator = mock(TsBehaviorEventCoordinator.class);
        TsBehaviorTrackingAspect aspect =
                new TsBehaviorTrackingAspect(coordinator, new ObjectMapper());
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TrackedService());
        proxyFactory.addAspect(aspect);
        service = proxyFactory.getProxy();
    }

    @Test
    void shouldExtractUserResourceAndOperationAfterSuccess() {
        LoginUser user = new LoginUser();
        user.setId("user-1");

        Result<String> result = service.like(user, 12L, true);

        assertEquals("点赞成功", result.getMessage());
        verify(coordinator).publishAfterCommit(argThat(event ->
                "like".equals(event.getEventType())
                        && "user-1".equals(event.getUserId())
                        && "feedback".equals(event.getResourceType())
                        && "12".equals(event.getResourceId())
                        && "SERVER".equals(event.getPlatform())
                        && event.getPropertiesJson().contains("TrackedService.like")));
    }

    @Test
    void shouldSkipEventWhenConditionIsFalse() {
        LoginUser user = new LoginUser();
        user.setId("user-1");

        service.like(user, 12L, false);

        verifyNoInteractions(coordinator);
    }

    @Test
    void shouldNotTrackFailedBusinessMethod() {
        LoginUser user = new LoginUser();
        user.setId("user-1");

        assertThrows(IllegalStateException.class, () -> service.fail(user));

        verifyNoInteractions(coordinator);
    }

    static class TrackedService {

        @TsBehaviorTrack(
                eventType = "like",
                resourceType = "feedback",
                resourceIdExpression = "#feedbackId",
                condition = "#result.message == '点赞成功'")
        public Result<String> like(LoginUser user, Long feedbackId, boolean first) {
            return Result.OK(first ? "点赞成功" : "已点赞", "ok");
        }

        @TsBehaviorTrack(eventType = "generate", resourceType = "role")
        public Result<String> fail(LoginUser user) {
            throw new IllegalStateException("failed");
        }
    }
}
