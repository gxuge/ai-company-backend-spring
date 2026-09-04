package org.jeecg.modules.system.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserfollow.TsUserFollowActionDto;
import org.jeecg.modules.system.mapper.TsUserFollowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 用户关注服务测试。 */
class TsUserFollowServiceImplTest {

    private TsUserFollowMapper followMapper;
    private TsUserFollowServiceImpl service;

    /** 初始化关注服务及依赖。 */
    @BeforeEach
    void setUp() {
        followMapper = mock(TsUserFollowMapper.class);
        service = new TsUserFollowServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", followMapper);
    }

    /** 用户不能关注自己。 */
    @Test
    void followShouldRejectSelf() {
        assertThrows(JeecgBootException.class, () -> service.follow(user(), request("u1")));

        verify(followMapper, never()).upsertFollow(any(), any(), any());
    }

    /** 目标用户不可用时不能新增关注。 */
    @Test
    void followShouldRejectUnavailableUser() {
        when(followMapper.countAvailableUser("u2")).thenReturn(0);

        assertThrows(JeecgBootException.class, () -> service.follow(user(), request("u2")));

        verify(followMapper, never()).upsertFollow(any(), any(), any());
    }

    /** 正常关注应幂等写入并返回最新状态。 */
    @Test
    void followShouldUpsertAndBuildStatus() {
        when(followMapper.countAvailableUser("u2")).thenReturn(1);
        when(followMapper.countActiveFollow("u1", "u2")).thenReturn(1);
        when(followMapper.countFollowers("u2")).thenReturn(3L);
        when(followMapper.countFollowing("u2")).thenReturn(4L);

        service.follow(user(), request("u2"));

        verify(followMapper).upsertFollow(eq("u1"), eq("u2"), any(Date.class));
        verify(followMapper).countActiveFollow("u1", "u2");
        verify(followMapper).countFollowers("u2");
        verify(followMapper).countFollowing("u2");
    }

    /** 重复取消关注应保持成功且不要求目标用户仍可用。 */
    @Test
    void unfollowShouldBeIdempotent() {
        when(followMapper.countActiveFollow("u1", "u2")).thenReturn(0);

        service.unfollow(user(), request("u2"));

        verify(followMapper).cancelFollow(eq("u1"), eq("u2"), any(Date.class));
        verify(followMapper, never()).countAvailableUser(any());
    }

    /** 构造登录用户。 */
    private LoginUser user() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        return user;
    }

    /** 构造关注操作参数。 */
    private TsUserFollowActionDto request(String targetUserId) {
        TsUserFollowActionDto request = new TsUserFollowActionDto();
        request.setTargetUserId(targetUserId);
        return request;
    }
}
