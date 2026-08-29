package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlExecutionQueryDto;
import org.jeecg.modules.system.vo.recommendetl.TsRecommendEtlExecutionVo;

/** 推荐 ETL 执行管理服务。 */
public interface ITsRecommendEtlExecutionService {
    /** 手动触发任务。 */
    TsRecommendEtlExecutionVo triggerManual(Long taskId);
    /** Quartz 触发任务。 */
    TsRecommendEtlExecutionVo triggerScheduled(Long taskId);
    /** 分页查询执行记录。 */
    Page<TsRecommendEtlExecutionVo> pageExecutions(
            TsRecommendEtlExecutionQueryDto request);
    /** 查询执行详情。 */
    TsRecommendEtlExecutionVo getExecution(Long id);
}
