package org.jeecg.modules.system.service.impl;

import org.jeecg.modules.system.constant.TsWorkReviewConstants;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.entity.TsWorkReview;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.jeecg.modules.system.mapper.TsStoryRoleRelMapper;
import org.jeecg.modules.system.mapper.TsWorkReviewItemMapper;
import org.jeecg.modules.system.mapper.TsWorkReviewLogMapper;
import org.jeecg.modules.system.mapper.TsWorkReviewMapper;
import org.jeecg.modules.system.review.TsWorkAiReviewer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TsWorkReviewServiceImplTest {
    private TsWorkReviewServiceImpl service;
    private TsRoleMapper roleMapper;
    private TsWorkReviewMapper reviewMapper;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        service = new TsWorkReviewServiceImpl();
        roleMapper = mock(TsRoleMapper.class);
        reviewMapper = mock(TsWorkReviewMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(service, "baseMapper", reviewMapper);
        ReflectionTestUtils.setField(service, "tsRoleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "tsStoryMapper", mock(TsStoryMapper.class));
        ReflectionTestUtils.setField(service, "tsStoryRoleRelMapper", mock(TsStoryRoleRelMapper.class));
        ReflectionTestUtils.setField(service, "tsWorkReviewItemMapper", mock(TsWorkReviewItemMapper.class));
        ReflectionTestUtils.setField(service, "tsWorkReviewLogMapper", mock(TsWorkReviewLogMapper.class));
        ReflectionTestUtils.setField(service, "tsWorkAiReviewer", mock(TsWorkAiReviewer.class));
        ReflectionTestUtils.setField(service, "applicationEventPublisher", eventPublisher);
    }

    @Test
    void submitRoleFreezesVersionOneSnapshotAndMakesWorkPrivate() throws Exception {
        TsRole role = new TsRole();
        role.setId(12L);
        role.setUserId("u1");
        role.setRoleName("林汐");
        role.setBackgroundStory("固定背景");
        role.setAvatarUrl("https://oss.example/role.png");
        role.setStatus(1);
        role.setIsPublic(1);
        role.setContentVersion(0);
        role.setDesiredPublic(1);
        when(roleMapper.selectById(12L)).thenReturn(role);
        when(reviewMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(reviewMapper.insert(any(TsWorkReview.class))).thenAnswer(invocation -> {
            TsWorkReview inserted = invocation.getArgument(0);
            inserted.setId(88L);
            return 1;
        });

        TsWorkReview review = service.submitRole(12L, 1);

        assertEquals(1, review.getWorkVersion());
        assertEquals(TsWorkReviewConstants.PENDING_AI, review.getStatus());
        assertEquals(sha256(review.getSnapshotJson()), review.getSnapshotHash());
        assertEquals(1, role.getContentVersion());
        assertEquals(0, role.getIsPublic());
        assertEquals(88L, role.getCurrentReviewId());
        assertEquals(TsWorkReviewConstants.PENDING_AI, role.getReviewStatus());
        assertNotNull(review.getReviewNo());
        verify(roleMapper).updateById(role);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    private String sha256(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte item : bytes) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
    }
}
