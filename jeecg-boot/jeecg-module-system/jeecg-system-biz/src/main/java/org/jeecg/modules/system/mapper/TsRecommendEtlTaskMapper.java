package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsRecommendEtlTask;

/** 推荐 ETL 任务 Mapper。 */
public interface TsRecommendEtlTaskMapper extends BaseMapper<TsRecommendEtlTask> {
    /** 原子占用任务，返回 1 表示成功。 */
    int occupy(@Param("taskId") Long taskId, @Param("executionId") Long executionId);

    /** 仅由当前执行记录释放任务占位。 */
    int release(@Param("taskId") Long taskId, @Param("executionId") Long executionId);
}
