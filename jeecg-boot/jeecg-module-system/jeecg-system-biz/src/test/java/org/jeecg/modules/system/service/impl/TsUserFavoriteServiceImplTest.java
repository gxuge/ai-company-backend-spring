package org.jeecg.modules.system.service.impl;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.behavior.TsBehaviorEventReporter;
import org.jeecg.modules.system.dto.tsuserfavorite.TsUserFavoriteActionDto;
import org.jeecg.modules.system.enums.tsbehavior.TsBehaviorEventType;
import org.jeecg.modules.system.mapper.TsUserFavoriteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 用户收藏行为事件测试。 */
class TsUserFavoriteServiceImplTest {
    private TsUserFavoriteMapper favoriteMapper;
    private TsBehaviorEventReporter behaviorEventReporter;
    private TsUserFavoriteServiceImpl service;

    /** 初始化收藏服务及依赖。 */
    @BeforeEach
    void setUp() {
        favoriteMapper = mock(TsUserFavoriteMapper.class);
        behaviorEventReporter = mock(TsBehaviorEventReporter.class);
        service = new TsUserFavoriteServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", favoriteMapper);
        ReflectionTestUtils.setField(
                service, "behaviorEventReporter", behaviorEventReporter);
    }

    /** 实际取消有效收藏后应发布 unfavorite。 */
    @Test
    void cancelFavoriteShouldReportWhenStateChanges() {
        when(favoriteMapper.cancelFavorite(
                eq("u1"), eq("role"), eq(1L), any(Date.class)))
                .thenReturn(1);

        service.cancelFavorite(user(), request());

        verify(behaviorEventReporter).reportAfterCommit(
                "u1", TsBehaviorEventType.UNFAVORITE, "role", 1L, Map.of());
    }

    /** 重复取消收藏时不得产生重复 unfavorite。 */
    @Test
    void cancelFavoriteShouldNotReportWhenAlreadyCanceled() {
        when(favoriteMapper.cancelFavorite(
                eq("u1"), eq("role"), eq(1L), any(Date.class)))
                .thenReturn(0);

        service.cancelFavorite(user(), request());

        verify(behaviorEventReporter, never()).reportAfterCommit(
                any(), any(), any(), any(), any());
    }

    /** 构造登录用户。 */
    private LoginUser user() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        return user;
    }

    /** 构造角色收藏参数。 */
    private TsUserFavoriteActionDto request() {
        TsUserFavoriteActionDto request = new TsUserFavoriteActionDto();
        request.setResourceType("role");
        request.setResourceId(1L);
        return request;
    }
}
