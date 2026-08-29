package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.config.TsRecommendEtlConfig;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlTaskQueryDto;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlTaskSaveDto;
import org.jeecg.modules.system.entity.TsRecommendEtlTask;
import org.jeecg.modules.system.mapper.TsRecommendEtlTaskMapper;
import org.jeecg.modules.system.recommendetl.TsRecommendEtlQuartzScheduler;
import org.jeecg.modules.system.service.ITsRecommendEtlTaskService;
import org.jeecg.modules.system.utils.recommendetl.TsRecommendEtlConverter;
import org.jeecg.modules.system.utils.recommendetl.TsRecommendEtlTransactionUtils;
import org.jeecg.modules.system.vo.recommendetl.TsRecommendEtlTaskVo;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 推荐 ETL 任务管理服务实现。 */
@Service
public class TsRecommendEtlTaskServiceImpl
        extends ServiceImpl<TsRecommendEtlTaskMapper, TsRecommendEtlTask>
        implements ITsRecommendEtlTaskService {
    private final ObjectMapper objectMapper;
    private final TsRecommendEtlConfig config;
    private final TsRecommendEtlQuartzScheduler quartzScheduler;

    /** 注入 JSON、配置和 Quartz 调度器。 */
    public TsRecommendEtlTaskServiceImpl(
            ObjectMapper objectMapper,
            TsRecommendEtlConfig config,
            TsRecommendEtlQuartzScheduler quartzScheduler) {
        this.objectMapper = objectMapper;
        this.config = config;
        this.quartzScheduler = quartzScheduler;
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsRecommendEtlTaskVo> pageTasks(
            TsRecommendEtlTaskQueryDto request) {
        TsRecommendEtlTaskQueryDto query =
                request == null ? new TsRecommendEtlTaskQueryDto() : request;
        LambdaQueryWrapper<TsRecommendEtlTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(TsRecommendEtlTask::getTaskName, query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getRecommendType())) {
            wrapper.eq(TsRecommendEtlTask::getRecommendType,
                    normalizeType(query.getRecommendType()));
        }
        if (query.getEnabled() != null) {
            wrapper.eq(TsRecommendEtlTask::getEnabled, query.getEnabled());
        }
        wrapper.orderByDesc(TsRecommendEtlTask::getCreateTime)
                .orderByDesc(TsRecommendEtlTask::getId);
        Page<TsRecommendEtlTask> source = page(
                new Page<>(pageNo(query.getPageNo()), pageSize(query.getPageSize())),
                wrapper);
        return convertTaskPage(source);
    }

    /** {@inheritDoc} */
    @Override
    public TsRecommendEtlTaskVo getTask(Long id) {
        return TsRecommendEtlConverter.toTaskVo(requireTask(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsRecommendEtlTaskVo createTask(TsRecommendEtlTaskSaveDto request) {
        TsRecommendEtlTask task = new TsRecommendEtlTask();
        applyAndValidate(task, request);
        task.setCreateTime(new Date()).setUpdateTime(new Date()).setDelFlag(0);
        save(task);
        TsRecommendEtlTransactionUtils.afterCommit(
                () -> quartzScheduler.synchronize(task));
        return TsRecommendEtlConverter.toTaskVo(task);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsRecommendEtlTaskVo updateTask(TsRecommendEtlTaskSaveDto request) {
        if (request.getId() == null) {
            throw new JeecgBootException("更新任务时 ID 不能为空");
        }
        TsRecommendEtlTask task = requireTask(request.getId());
        if (task.getRunningExecutionId() != null) {
            throw new JeecgBootException("任务正在执行，不能编辑");
        }
        applyAndValidate(task, request);
        task.setUpdateTime(new Date());
        updateById(task);
        TsRecommendEtlTransactionUtils.afterCommit(
                () -> quartzScheduler.synchronize(task));
        return TsRecommendEtlConverter.toTaskVo(task);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        TsRecommendEtlTask task = requireTask(id);
        if (task.getRunningExecutionId() != null) {
            throw new JeecgBootException("任务正在执行，不能删除");
        }
        removeById(id);
        TsRecommendEtlTransactionUtils.afterCommit(
                () -> quartzScheduler.delete(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsRecommendEtlTaskVo toggleTask(Long id, Integer enabled) {
        TsRecommendEtlTask task = requireTask(id);
        if (!Integer.valueOf(0).equals(enabled)
                && !Integer.valueOf(1).equals(enabled)) {
            throw new JeecgBootException("启停状态只能是 0 或 1");
        }
        if (Integer.valueOf(1).equals(enabled)
                && !StringUtils.hasText(task.getCronExpression())) {
            throw new JeecgBootException("启用定时任务前必须配置 Cron 表达式");
        }
        task.setEnabled(enabled).setUpdateTime(new Date());
        updateById(task);
        TsRecommendEtlTransactionUtils.afterCommit(
                () -> quartzScheduler.synchronize(task));
        return TsRecommendEtlConverter.toTaskVo(task);
    }

    /** 应用请求字段并执行任务级业务校验。 */
    private void applyAndValidate(
            TsRecommendEtlTask task,
            TsRecommendEtlTaskSaveDto request) {
        task.setTaskName(request.getTaskName().trim())
                .setRecommendType(normalizeType(request.getRecommendType()))
                .setTimeRangeMode(normalizeRangeMode(request.getTimeRangeMode()))
                .setStartTime(request.getStartTime())
                .setEndTime(request.getEndTime())
                .setRecentDays(request.getRecentDays())
                .setScriptPath(request.getScriptPath().trim())
                .setOutputDir(request.getOutputDir().trim())
                .setStorageType(normalizeStorage(request.getStorageType()))
                .setTrainRatio(request.getTrainRatio() == null
                        ? new BigDecimal("0.9") : request.getTrainRatio())
                .setEvalRatio(request.getEvalRatio() == null
                        ? new BigDecimal("0.1") : request.getEvalRatio())
                .setRunParamsJson(trimToNull(request.getRunParamsJson()))
                .setCronExpression(trimToNull(request.getCronExpression()))
                .setEnabled(request.getEnabled() == null ? 0 : request.getEnabled())
                .setTimeoutSeconds(request.getTimeoutSeconds() == null
                        ? config.getDefaultTimeoutSeconds()
                        : request.getTimeoutSeconds());
        validateTask(task);
    }

    /** 校验时间、切分比例、Cron、附加参数和超时边界。 */
    private void validateTask(TsRecommendEtlTask task) {
        if ("FIXED".equals(task.getTimeRangeMode())) {
            if (task.getStartTime() == null || task.getEndTime() == null
                    || !task.getStartTime().before(task.getEndTime())) {
                throw new JeecgBootException("固定时间范围必须满足开始时间早于结束时间");
            }
        } else if (task.getRecentDays() == null || task.getRecentDays() < 1) {
            throw new JeecgBootException("最近天数必须大于 0");
        }
        if (task.getTrainRatio().add(task.getEvalRatio())
                .compareTo(BigDecimal.ONE) != 0) {
            throw new JeecgBootException("train/eval 比例之和必须等于 1");
        }
        if (Integer.valueOf(1).equals(task.getEnabled())
                && !StringUtils.hasText(task.getCronExpression())) {
            throw new JeecgBootException("启用任务时必须配置 Cron 表达式");
        }
        if (StringUtils.hasText(task.getCronExpression())
                && !CronExpression.isValidExpression(task.getCronExpression())) {
            throw new JeecgBootException("Cron 表达式不合法");
        }
        if (task.getTimeoutSeconds() < 10
                || task.getTimeoutSeconds() > config.getMaxTimeoutSeconds()) {
            throw new JeecgBootException("任务超时时间超出允许范围");
        }
        validateRunParams(task.getRunParamsJson());
    }

    /** 校验附加运行参数必须是 JSON 对象。 */
    private void validateRunParams(String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            if (!node.isObject()) {
                throw new JeecgBootException("附加运行参数必须是 JSON 对象");
            }
        } catch (JeecgBootException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JeecgBootException("附加运行参数 JSON 不合法", exception);
        }
    }

    /** 查询存在且未删除的任务。 */
    private TsRecommendEtlTask requireTask(Long id) {
        if (id == null || id <= 0) {
            throw new JeecgBootException("任务 ID 不合法");
        }
        TsRecommendEtlTask task = getById(id);
        if (task == null) {
            throw new JeecgBootException("推荐 ETL 任务不存在");
        }
        return task;
    }

    /** 归一化推荐类型。 */
    private String normalizeType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ROLE", "STORY").contains(normalized)) {
            throw new JeecgBootException("推荐类型只能是 ROLE 或 STORY");
        }
        return normalized;
    }

    /** 归一化时间范围模式。 */
    private String normalizeRangeMode(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("FIXED", "RECENT_DAYS").contains(normalized)) {
            throw new JeecgBootException("时间范围模式不合法");
        }
        return normalized;
    }

    /** 归一化存储类型。 */
    private String normalizeStorage(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("LOCAL", "OSS").contains(normalized)) {
            throw new JeecgBootException("存储类型只能是 LOCAL 或 OSS");
        }
        return normalized;
    }

    /** 空白字符串转 null。 */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 转换任务分页结果。 */
    private Page<TsRecommendEtlTaskVo> convertTaskPage(
            Page<TsRecommendEtlTask> source) {
        Page<TsRecommendEtlTaskVo> target =
                new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(source.getRecords().stream()
                .map(TsRecommendEtlConverter::toTaskVo)
                .toList());
        return target;
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
