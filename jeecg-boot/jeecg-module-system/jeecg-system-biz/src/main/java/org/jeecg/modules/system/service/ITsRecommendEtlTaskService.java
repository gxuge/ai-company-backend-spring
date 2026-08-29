package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlTaskQueryDto;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlTaskSaveDto;
import org.jeecg.modules.system.vo.recommendetl.TsRecommendEtlTaskVo;

/** 推荐 ETL 任务管理服务。 */
public interface ITsRecommendEtlTaskService {
    /** 分页查询任务。 */
    Page<TsRecommendEtlTaskVo> pageTasks(TsRecommendEtlTaskQueryDto request);
    /** 查询任务详情。 */
    TsRecommendEtlTaskVo getTask(Long id);
    /** 新增任务。 */
    TsRecommendEtlTaskVo createTask(TsRecommendEtlTaskSaveDto request);
    /** 更新任务。 */
    TsRecommendEtlTaskVo updateTask(TsRecommendEtlTaskSaveDto request);
    /** 删除未运行任务。 */
    void deleteTask(Long id);
    /** 启用或停用任务。 */
    TsRecommendEtlTaskVo toggleTask(Long id, Integer enabled);
}
