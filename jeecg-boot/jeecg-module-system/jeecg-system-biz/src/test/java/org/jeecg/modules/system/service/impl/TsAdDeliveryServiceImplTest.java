package org.jeecg.modules.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsad.TsAdEventReportDto;
import org.jeecg.modules.system.entity.TsAdContent;
import org.jeecg.modules.system.entity.TsAdSlot;
import org.jeecg.modules.system.mapper.TsAdContentMapper;
import org.jeecg.modules.system.mapper.TsAdQueryMapper;
import org.jeecg.modules.system.mapper.TsAdSlotMapper;
import org.jeecg.modules.system.po.tsad.TsAdDeliveryCandidatePo;
import org.jeecg.modules.system.service.ITsMemberService;
import org.jeecg.modules.system.vo.tsad.TsAdSlotDeliveryVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberCurrentVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 前端广告投放服务测试。 */
class TsAdDeliveryServiceImplTest {
    private TsAdQueryMapper queryMapper;
    private TsAdContentMapper contentMapper;
    private TsAdSlotMapper slotMapper;
    private ITsMemberService memberService;
    private TsAdDeliveryServiceImpl service;

    /** 初始化投放服务依赖。 */
    @BeforeEach
    void setUp() {
        queryMapper = mock(TsAdQueryMapper.class);
        contentMapper = mock(TsAdContentMapper.class);
        slotMapper = mock(TsAdSlotMapper.class);
        memberService = mock(ITsMemberService.class);
        service = new TsAdDeliveryServiceImpl(
                queryMapper, contentMapper, slotMapper, memberService, new ObjectMapper());
    }

    /** 匿名用户只应看到匿名或全用户内容。 */
    @Test
    void deliverShouldFilterAnonymousAudience() {
        when(queryMapper.selectDeliveryCandidates(any(), any())).thenReturn(List.of(
                candidate(1L, "ANONYMOUS", "[\"ALL\"]", null),
                candidate(2L, "LOGIN", "[\"ALL\"]", null)));

        List<TsAdSlotDeliveryVo> result =
                service.deliver(List.of("HOME_BANNER"), "WEB", null);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getContents().size());
        assertEquals(1L, result.get(0).getContents().get(0).getId());
    }

    /** 登录PRO用户必须同时满足会员和指定用户规则。 */
    @Test
    void deliverShouldFilterMemberAndUserList() {
        LoginUser user = new LoginUser().setId("u1");
        TsMemberCurrentVo membership = new TsMemberCurrentVo();
        membership.setPlanCode("PRO");
        when(memberService.getCurrentMembership(user)).thenReturn(membership);
        when(queryMapper.selectDeliveryCandidates(any(), any())).thenReturn(List.of(
                candidate(1L, "USER_LIST", "[\"PRO\"]", "[\"u1\"]"),
                candidate(2L, "USER_LIST", "[\"ULTRA\"]", "[\"u1\"]"),
                candidate(3L, "USER_LIST", "[\"PRO\"]", "[\"u2\"]")));

        List<TsAdSlotDeliveryVo> result =
                service.deliver(List.of("HOME_BANNER"), "WEB", user);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getContents().size());
        assertEquals(1L, result.get(0).getContents().get(0).getId());
    }

    /** 相同eventId重复上报时第二次必须返回未首次接受。 */
    @Test
    void reportEventShouldExposeIdempotentInsertResult() {
        when(contentMapper.selectById(1L))
                .thenReturn(new TsAdContent().setId(1L).setSlotId(2L));
        when(slotMapper.selectOne(any()))
                .thenReturn(new TsAdSlot().setId(2L).setSlotCode("HOME_BANNER"));
        when(queryMapper.insertEventIgnore(any())).thenReturn(1, 0);
        TsAdEventReportDto request = new TsAdEventReportDto();
        request.setEventId("event-1");
        request.setContentId(1L);
        request.setSlotCode("HOME_BANNER");
        request.setEventType("IMPRESSION");
        request.setVisitorId("visitor-1");
        request.setPlatform("WEB");

        assertTrue(service.reportEvent(request, null));
        assertFalse(service.reportEvent(request, null));
    }

    /** 构造投放候选内容。 */
    private TsAdDeliveryCandidatePo candidate(
            Long id, String audienceType, String memberLevels, String userIds) {
        TsAdDeliveryCandidatePo candidate = new TsAdDeliveryCandidatePo();
        candidate.setSlotId(10L);
        candidate.setSlotCode("HOME_BANNER");
        candidate.setSlotType("BANNER");
        candidate.setMaxItems(10);
        candidate.setContentId(id);
        candidate.setContentCode("AD_" + id);
        candidate.setTitle("广告" + id);
        candidate.setMediaType("IMAGE");
        candidate.setImageUrl("https://example.com/" + id + ".png");
        candidate.setLinkType("NONE");
        candidate.setPlatformJson("[\"WEB\"]");
        candidate.setAudienceType(audienceType);
        candidate.setMemberLevelJson(memberLevels);
        candidate.setUserIdJson(userIds);
        return candidate;
    }
}
