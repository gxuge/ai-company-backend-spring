package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.config.TsRecommendEtlConfig;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlExecutionQueryDto;
import org.jeecg.modules.system.entity.TsRecommendEtlExecution;
import org.jeecg.modules.system.entity.TsRecommendEtlTask;
import org.jeecg.modules.system.enums.recommendetl.TsRecommendEtlStatus;
import org.jeecg.modules.system.enums.recommendetl.TsRecommendEtlTriggerType;
import org.jeecg.modules.system.mapper.TsRecommendEtlExecutionMapper;
import org.jeecg.modules.system.mapper.TsRecommendEtlTaskMapper;
import org.jeecg.modules.system.recommendetl.TsRecommendEtlExecutionDispatcher;
import org.jeecg.modules.system.service.ITsRecommendEtlExecutionService;
import org.jeecg.modules.system.utils.recommendetl.TsRecommendEtlConverter;
import org.jeecg.modules.system.utils.recommendetl.TsRecommendEtlTransactionUtils;
import org.jeecg.modules.system.vo.recommendetl.TsRecommendEtlExecutionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 推荐 ETL 执行管理服务实现。 */
@Service
public class TsRecommendEtlExecutionServiceImpl
        extends ServiceImpl<TsRecommendEtlExecutionMapper, TsRecommendEtlExecution>
        implements ITsRecommendEtlExecutionService {
    private final TsRecommendEtlTaskMapper taskMapper;
    private final TsRecommendEtlExecutionDispatcher dispatcher;
    private final TsRecommendEtlConfig config;
    private final ObjectMapper objectMapper;

    /** 注入任务 Mapper、执行分发器、配置和 JSON 组件。 */
    public TsRecommendEtlExecutionServiceImpl(
            TsRecommendEtlTaskMapper taskMapper,
            TsRecommendEtlExecutionDispatcher dispatcher,
            TsRecommendEtlConfig config,
            ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.dispatcher = dispatcher;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsRecommendEtlExecutionVo triggerManual(Long taskId) {
        return trigger(taskId, TsRecommendEtlTriggerType.MANUAL, false);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsRecommendEtlExecutionVo triggerScheduled(Long taskId) {
        return trigger(taskId, TsRecommendEtlTriggerType.SCHEDULED, true);
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsRecommendEtlExecutionVo> pageExecutions(
            TsRecommendEtlExecutionQueryDto request) {
        TsRecommendEtlExecutionQueryDto query = request == null
                ? new TsRecommendEtlExecutionQueryDto() : request;
        LambdaQueryWrapper<TsRecommendEtlExecution> wrapper =
                new LambdaQueryWrapper<>();
        if (query.getTaskId() != null) {
            wrapper.eq(TsRecommendEtlExecution::getTaskId, query.getTaskId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(TsRecommendEtlExecution::getTaskName,
                    query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getRecommendType())) {
            wrapper.eq(TsRecommendEtlExecution::getRecommendType,
                    normalize(query.getRecommendType(),
                            List.of("ROLE", "STORY"), "推荐类型"));
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(TsRecommendEtlExecution::getStatus,
                    normalize(query.getStatus(),
                            List.of("WAITING", "RUNNING", "SUCCESS", "FAILED"),
                            "执行状态"));
        }
        if (StringUtils.hasText(query.getTriggerType())) {
            wrapper.eq(TsRecommendEtlExecution::getTriggerType,
                    normalize(query.getTriggerType(),
                            List.of("MANUAL", "SCHEDULED"), "触发类型"));
        }
        wrapper.orderByDesc(TsRecommendEtlExecution::getCreateTime)
                .orderByDesc(TsRecommendEtlExecution::getId);
        Page<TsRecommendEtlExecution> source = page(
                new Page<>(pageNo(query.getPageNo()), pageSize(query.getPageSize())),
                wrapper);
        Page<TsRecommendEtlExecutionVo> target =
                new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(source.getRecords().stream()
                .map(TsRecommendEtlConverter::toExecutionVo)
                .toList());
        return target;
    }

    /** {@inheritDoc} */
    @Override
    public TsRecommendEtlExecutionVo getExecution(Long id) {
        if (id == null || id <= 0) {
            throw new JeecgBootException("执行记录 ID 不合法");
        }
        TsRecommendEtlExecution execution = getById(id);
        if (execution == null) {
            throw new JeecgBootException("推荐 ETL 执行记录不存在");
        }
        return TsRecommendEtlConverter.toExecutionVo(execution);
    }

    /** 创建 WAITING 记录、原子占位并在事务提交后分发。 */
    private TsRecommendEtlExecutionVo trigger(
            Long taskId,
            TsRecommendEtlTriggerType triggerType,
            boolean requireEnabled) {
        if (!config.isEnabled()) {
            throw new JeecgBootException("推荐 ETL 功能已关闭");
        }
        TsRecommendEtlTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new JeecgBootException("推荐 ETL 任务不存在");
        }
        if (requireEnabled && !Integer.valueOf(1).equals(task.getEnabled())) {
            throw new JeecgBootException("定时任务已停用");
        }
        long executionId = IdWorker.getId();
        if (taskMapper.occupy(taskId, executionId) != 1) {
            throw new JeecgBootException("任务正在执行，请勿重复触发");
        }
        Date end = new Date();
        Date start = resolveStart(task, end);
        TsRecommendEtlExecution execution = new TsRecommendEtlExecution()
                .setId(executionId)
                .setTaskId(taskId)
                .setTaskName(task.getTaskName())
                .setRecommendType(task.getRecommendType())
                .setTriggerType(triggerType.name())
                .setStatus(TsRecommendEtlStatus.WAITING.name())
                .setRangeStartTime(start)
                .setRangeEndTime(resolveEnd(task, end))
                .setArgumentsJson(argumentsSnapshot(task, executionId))
                .setCreateTime(new Date())
                .setUpdateTime(new Date());
        save(execution);
        TsRecommendEtlTransactionUtils.afterCommit(
                () -> dispatchSafely(execution));
        return TsRecommendEtlConverter.toExecutionVo(execution);
    }

    /** 事务提交后分发；分发失败时立即结束记录并释放占位。 */
    private void dispatchSafely(TsRecommendEtlExecution execution) {
        try {
            dispatcher.dispatch(execution.getId());
        } catch (Exception exception) {
            Date finishedAt = new Date();
            execution.setStatus(TsRecommendEtlStatus.FAILED.name())
                    .setFinishedAt(finishedAt)
                    .setDurationMs(0L)
                    .setErrorCode("DISPATCH_FAILED")
                    .setErrorMessage(exception.getMessage())
                    .setUpdateTime(finishedAt);
            updateById(execution);
            taskMapper.release(execution.getTaskId(), execution.getId());
        }
    }

    /** 解析本次执行开始时间。 */
    private Date resolveStart(TsRecommendEtlTask task, Date now) {
        if ("FIXED".equals(task.getTimeRangeMode())) {
            return task.getStartTime();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, -task.getRecentDays());
        return calendar.getTime();
    }

    /** 解析本次执行结束时间。 */
    private Date resolveEnd(TsRecommendEtlTask task, Date now) {
        return "FIXED".equals(task.getTimeRangeMode())
                ? task.getEndTime() : now;
    }

    /** 生成不含敏感信息的执行参数快照。 */
    private String argumentsSnapshot(TsRecommendEtlTask task, long executionId) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("executionId", executionId);
            snapshot.put("scriptPath", task.getScriptPath());
            snapshot.put("outputDir", task.getOutputDir());
            snapshot.put("storageType", task.getStorageType());
            snapshot.put("trainRatio", task.getTrainRatio());
            snapshot.put("evalRatio", task.getEvalRatio());
            snapshot.put("runParams", StringUtils.hasText(task.getRunParamsJson())
                    ? objectMapper.readTree(task.getRunParamsJson()) : null);
            snapshot.put("timeoutSeconds", task.getTimeoutSeconds());
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            throw new JeecgBootException("生成 ETL 参数快照失败", exception);
        }
    }

    /** 校验并归一化枚举字符串。 */
    private String normalize(
            String value,
            List<String> allowed,
            String label) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new JeecgBootException(label + "不合法");
        }
        return normalized;
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
