package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventAdminQueryDto;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventRetryDto;
import org.jeecg.modules.system.enums.tsreward.TsRewardErrorCode;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventStatus;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventType;
import org.jeecg.modules.system.exception.tsreward.TsRewardBizException;
import org.jeecg.modules.system.mapper.TsRewardEventQueryMapper;
import org.jeecg.modules.system.reward.TsRewardEventExecutor;
import org.jeecg.modules.system.service.ITsRewardEventAdminService;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventAdminDetailVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventAdminItemVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventSummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/** 统一奖励事件后台管理服务实现。 */
@Service
public class TsRewardEventAdminServiceImpl implements ITsRewardEventAdminService {

    private final TsRewardEventQueryMapper queryMapper;
    private final TsRewardEventExecutor eventExecutor;

    /** 注入奖励事件查询与执行依赖。 */
    public TsRewardEventAdminServiceImpl(
            TsRewardEventQueryMapper queryMapper,
            TsRewardEventExecutor eventExecutor) {
        this.queryMapper = queryMapper;
        this.eventExecutor = eventExecutor;
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsRewardEventAdminItemVo> pageEvents(
            TsRewardEventAdminQueryDto request) {
        TsRewardEventAdminQueryDto query = normalize(request);
        return queryMapper.selectAdminPage(
                new Page<>(pageNo(query.getPageNo()), pageSize(query.getPageSize())),
                query);
    }

    /** {@inheritDoc} */
    @Override
    public TsRewardEventSummaryVo summarizeEvents(
            TsRewardEventAdminQueryDto request) {
        return queryMapper.selectAdminSummary(normalize(request));
    }

    /** {@inheritDoc} */
    @Override
    public TsRewardEventAdminDetailVo getEvent(Long id) {
        if (id == null || id <= 0) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_INVALID,
                    "奖励事件ID不合法");
        }
        TsRewardEventAdminDetailVo detail = queryMapper.selectAdminDetail(id);
        if (detail == null) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_NOT_FOUND,
                    "奖励事件不存在");
        }
        return detail;
    }

    /** {@inheritDoc} */
    @Override
    public TsRewardEventResultVo retryEvent(TsRewardEventRetryDto request) {
        return eventExecutor.retryFailedEvent(request.getEventId());
    }

    /** 校验并归一化后台奖励事件查询参数。 */
    private TsRewardEventAdminQueryDto normalize(
            TsRewardEventAdminQueryDto request) {
        TsRewardEventAdminQueryDto query =
                request == null ? new TsRewardEventAdminQueryDto() : request;
        if (StringUtils.hasText(query.getKeyword())) {
            query.setKeyword(query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getEventType())) {
            String eventType = query.getEventType().trim().toUpperCase(Locale.ROOT);
            try {
                TsRewardEventType.valueOf(eventType);
            } catch (IllegalArgumentException exception) {
                throw new TsRewardBizException(
                        TsRewardErrorCode.REWARD_EVENT_TYPE_UNSUPPORTED,
                        "奖励事件类型不合法");
            }
            query.setEventType(eventType);
        }
        if (StringUtils.hasText(query.getStatus())) {
            String status = query.getStatus().trim().toUpperCase(Locale.ROOT);
            try {
                TsRewardEventStatus.valueOf(status);
            } catch (IllegalArgumentException exception) {
                throw new TsRewardBizException(
                        TsRewardErrorCode.REWARD_EVENT_STATUS_INVALID,
                        "奖励事件状态不合法");
            }
            query.setStatus(status);
        }
        return query;
    }

    /** 归一化页码。 */
    private int pageNo(Integer value) {
        return value == null ? 1 : Math.max(value, 1);
    }

    /** 归一化分页大小。 */
    private int pageSize(Integer value) {
        return value == null ? 10 : Math.min(Math.max(value, 1), 100);
    }
}
