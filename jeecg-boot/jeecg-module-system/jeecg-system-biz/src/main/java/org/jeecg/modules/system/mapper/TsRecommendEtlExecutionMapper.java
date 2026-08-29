package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsRecommendEtlExecution;

import java.util.Date;

/** 推荐 ETL 执行记录 Mapper。 */
public interface TsRecommendEtlExecutionMapper extends BaseMapper<TsRecommendEtlExecution> {
    /** 将 WAITING 记录原子切换为 RUNNING。 */
    int markRunning(@Param("id") Long id, @Param("startedAt") Date startedAt);
}
