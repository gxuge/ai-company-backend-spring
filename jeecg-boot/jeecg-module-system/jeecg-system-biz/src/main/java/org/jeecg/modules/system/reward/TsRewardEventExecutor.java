package org.jeecg.modules.system.reward;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventCommand;
import org.jeecg.modules.system.entity.TsRewardEvent;
import org.jeecg.modules.system.enums.tsreward.TsRewardErrorCode;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventStatus;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventType;
import org.jeecg.modules.system.exception.tsactivity.TsActivityBizException;
import org.jeecg.modules.system.exception.tspoints.TsPointsBizException;
import org.jeecg.modules.system.exception.tsreward.TsRewardBizException;
import org.jeecg.modules.system.mapper.TsRewardEventMapper;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/** 持久化并幂等执行统一奖励事件。 */
@Slf4j
@Component
public class TsRewardEventExecutor {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;
    private static final int MAX_RETRY_COUNT_LIMIT = 10;

    private final TsRewardEventMapper eventMapper;
    private final TsRewardEventDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    /** 注入奖励事件持久化、分发和JSON转换依赖。 */
    public TsRewardEventExecutor(
            TsRewardEventMapper eventMapper,
            TsRewardEventDispatcher dispatcher,
            ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行奖励事件；存在外层事务时自动加入外层事务，不存在时事件状态独立持久化。
     */
    public TsRewardEventResultVo execute(TsRewardEventCommand command) {
        validateCommand(command);
        TsRewardEvent event = selectByEventId(command.getEventId());
        if (event == null) {
            event = insertEvent(command);
        } else {
            verifySameEvent(event, command);
        }
        if (TsRewardEventStatus.SUCCESS.name().equals(event.getStatus())) {
            return readResult(event);
        }
        if (event.getRetryCount() >= event.getMaxRetryCount()) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_RETRY_EXHAUSTED,
                    "奖励事件已达到最大执行次数");
        }
        return processEvent(event);
    }

    /** 小批量重试失败且未达到最大次数的奖励事件。 */
    public int retryFailed(int limit) {
        int batchSize = Math.max(1, Math.min(limit, 100));
        List<TsRewardEvent> events = eventMapper.selectList(
                Wrappers.<TsRewardEvent>lambdaQuery()
                        .eq(TsRewardEvent::getStatus, TsRewardEventStatus.FAILED.name())
                        .apply("retry_count < max_retry_count")
                        .orderByAsc(TsRewardEvent::getUpdatedAt)
                        .last("LIMIT " + batchSize));
        int successCount = 0;
        for (TsRewardEvent event : events) {
            try {
                processEvent(event);
                successCount++;
            } catch (RuntimeException exception) {
                log.warn(
                        "奖励事件重试失败，eventId={}, retryCount={}",
                        event.getEventId(),
                        event.getRetryCount(),
                        exception);
            }
        }
        return successCount;
    }

    /** 手动重试单个失败事件。 */
    public TsRewardEventResultVo retryFailedEvent(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_INVALID,
                    "奖励事件ID不能为空");
        }
        TsRewardEvent event = selectByEventId(eventId);
        if (event == null) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_NOT_FOUND,
                    "奖励事件不存在");
        }
        if (!TsRewardEventStatus.FAILED.name().equals(event.getStatus())) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_STATUS_INVALID,
                    "只有失败的奖励事件可以重试");
        }
        if (event.getRetryCount() >= event.getMaxRetryCount()) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_RETRY_EXHAUSTED,
                    "奖励事件已达到最大执行次数");
        }
        return processEvent(event);
    }

    /** 插入新事件，并在并发插入时读取已存在事件。 */
    private TsRewardEvent insertEvent(TsRewardEventCommand command) {
        Date now = new Date();
        TsRewardEvent event = new TsRewardEvent()
                .setEventId(command.getEventId().trim())
                .setEventType(command.getEventType().trim())
                .setUserId(command.getUserId().trim())
                .setBizId(command.getBizId().trim())
                .setPayloadJson(writeJson(command.getPayload()))
                .setStatus(TsRewardEventStatus.PENDING.name())
                .setRetryCount(0)
                .setMaxRetryCount(normalizeMaxRetryCount(command.getMaxRetryCount()))
                .setCreatedAt(now)
                .setUpdatedAt(now);
        try {
            eventMapper.insert(event);
            return event;
        } catch (DuplicateKeyException exception) {
            TsRewardEvent existing = selectByEventId(command.getEventId());
            if (existing == null) {
                throw exception;
            }
            verifySameEvent(existing, command);
            return existing;
        }
    }

    /** 标记执行状态、分发奖励，并保存成功或失败信息。 */
    private TsRewardEventResultVo processEvent(TsRewardEvent event) {
        Date now = new Date();
        event.setStatus(TsRewardEventStatus.PROCESSING.name())
                .setRetryCount(event.getRetryCount() + 1)
                .setLastErrorCode(null)
                .setLastErrorMessage(null)
                .setUpdatedAt(now);
        eventMapper.updateById(event);
        try {
            TsRewardEventResultVo result = dispatcher.dispatch(event);
            event.setStatus(TsRewardEventStatus.SUCCESS.name())
                    .setResultJson(writeJson(result))
                    .setProcessedAt(new Date())
                    .setUpdatedAt(new Date());
            eventMapper.updateById(event);
            return result;
        } catch (RuntimeException exception) {
            event.setStatus(TsRewardEventStatus.FAILED.name())
                    .setLastErrorCode(resolveErrorCode(exception))
                    .setLastErrorMessage(limitMessage(exception.getMessage()))
                    .setUpdatedAt(new Date());
            eventMapper.updateById(event);
            throw exception;
        }
    }

    /** 查询指定幂等奖励事件。 */
    private TsRewardEvent selectByEventId(String eventId) {
        return eventMapper.selectOne(Wrappers.<TsRewardEvent>lambdaQuery()
                .eq(TsRewardEvent::getEventId, eventId.trim())
                .last("LIMIT 1"));
    }

    /** 校验重复事件ID对应的业务语义完全一致。 */
    private void verifySameEvent(
            TsRewardEvent event,
            TsRewardEventCommand command) {
        if (!Objects.equals(event.getEventType(), command.getEventType().trim())
                || !Objects.equals(event.getUserId(), command.getUserId().trim())
                || !Objects.equals(event.getBizId(), command.getBizId().trim())) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_CONFLICT,
                    "同一奖励事件ID对应了不同业务请求");
        }
    }

    /** 读取已成功事件的持久化结果。 */
    private TsRewardEventResultVo readResult(TsRewardEvent event) {
        if (!StringUtils.hasText(event.getResultJson())) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_EXECUTION_FAILED,
                    "奖励事件成功结果缺失");
        }
        try {
            return objectMapper.readValue(
                    event.getResultJson(), TsRewardEventResultVo.class);
        } catch (JsonProcessingException exception) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_EXECUTION_FAILED,
                    "奖励事件结果无法解析");
        }
    }

    /** 将事件负载或结果序列化为JSON。 */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_PAYLOAD_INVALID,
                    "奖励事件数据无法序列化");
        }
    }

    /** 校验事件命令必填字段和事件类型。 */
    private void validateCommand(TsRewardEventCommand command) {
        if (command == null
                || !StringUtils.hasText(command.getEventId())
                || !StringUtils.hasText(command.getEventType())
                || !StringUtils.hasText(command.getUserId())
                || !StringUtils.hasText(command.getBizId())
                || command.getPayload() == null) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_INVALID,
                    "奖励事件参数不完整");
        }
        try {
            TsRewardEventType.valueOf(command.getEventType().trim());
        } catch (IllegalArgumentException exception) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_TYPE_UNSUPPORTED,
                    "不支持的奖励事件类型");
        }
    }

    /** 归一化最大执行次数。 */
    private int normalizeMaxRetryCount(Integer value) {
        if (value == null) {
            return DEFAULT_MAX_RETRY_COUNT;
        }
        return Math.max(1, Math.min(value, MAX_RETRY_COUNT_LIMIT));
    }

    /** 从领域异常中提取机器错误码。 */
    private String resolveErrorCode(RuntimeException exception) {
        if (exception instanceof TsPointsBizException pointsException) {
            return pointsException.getErrorCode().name();
        }
        if (exception instanceof TsActivityBizException activityException) {
            return activityException.getErrorCode().name();
        }
        if (exception instanceof TsRewardBizException rewardException) {
            return rewardException.getErrorCode().name();
        }
        return TsRewardErrorCode.REWARD_EVENT_EXECUTION_FAILED.name();
    }

    /** 截断数据库错误信息，避免异常文本超过字段长度。 */
    private String limitMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
