package org.jeecg.modules.system.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserresourcelike.TsUserResourceLikeActionDto;
import org.jeecg.modules.system.mapper.TsUserResourceLikeMapper;
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

/** 用户角色与故事点赞服务测试。 */
class TsUserResourceLikeServiceImplTest {

    private TsUserResourceLikeMapper likeMapper;
    private TsUserResourceLikeServiceImpl service;

    /** 初始化点赞服务及依赖。 */
    @BeforeEach
    void setUp() {
        likeMapper = mock(TsUserResourceLikeMapper.class);
        service = new TsUserResourceLikeServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", likeMapper);
    }

    /** 非公开资源不能新增点赞。 */
    @Test
    void likeShouldRejectUnavailableResource() {
        when(likeMapper.countAvailableResource("role", 1L)).thenReturn(0);

        assertThrows(JeecgBootException.class, () -> service.like(user(), request()));

        verify(likeMapper, never()).upsertLike(any(), any(), any(), any());
    }

    /** 正常点赞应幂等写入并返回最新状态。 */
    @Test
    void likeShouldUpsertAndBuildStatus() {
        when(likeMapper.countAvailableResource("role", 1L)).thenReturn(1);
        when(likeMapper.countActiveLike("u1", "role", 1L)).thenReturn(1);
        when(likeMapper.countResourceLikes("role", 1L)).thenReturn(8L);

        service.like(user(), request());

        verify(likeMapper).upsertLike(
                eq("u1"), eq("role"), eq(1L), any(Date.class));
        verify(likeMapper).countActiveLike("u1", "role", 1L);
        verify(likeMapper).countResourceLikes("role", 1L);
    }

    /** 重复取消点赞应保持成功且不要求资源仍在线。 */
    @Test
    void unlikeShouldBeIdempotent() {
        when(likeMapper.countActiveLike("u1", "role", 1L)).thenReturn(0);

        service.unlike(user(), request());

        verify(likeMapper).cancelLike(
                eq("u1"), eq("role"), eq(1L), any(Date.class));
        verify(likeMapper, never()).countAvailableResource(any(), any());
    }

    /** 构造登录用户。 */
    private LoginUser user() {
        LoginUser user = new LoginUser();
        user.setId("u1");
        return user;
    }

    /** 构造角色点赞参数。 */
    private TsUserResourceLikeActionDto request() {
        TsUserResourceLikeActionDto request = new TsUserResourceLikeActionDto();
        request.setResourceType("role");
        request.setResourceId(1L);
        return request;
    }
}
