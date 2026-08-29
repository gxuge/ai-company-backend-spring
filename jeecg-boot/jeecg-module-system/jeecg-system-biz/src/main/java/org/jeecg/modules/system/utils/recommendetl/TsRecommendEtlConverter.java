package org.jeecg.modules.system.utils.recommendetl;

import org.jeecg.modules.system.entity.TsRecommendEtlExecution;
import org.jeecg.modules.system.entity.TsRecommendEtlTask;
import org.jeecg.modules.system.vo.recommendetl.TsRecommendEtlExecutionVo;
import org.jeecg.modules.system.vo.recommendetl.TsRecommendEtlTaskVo;
import org.springframework.beans.BeanUtils;

/** 推荐 ETL 展示对象转换器。 */
public final class TsRecommendEtlConverter {
    private TsRecommendEtlConverter() {
    }

    /** 将任务实体转换为展示对象。 */
    public static TsRecommendEtlTaskVo toTaskVo(TsRecommendEtlTask source) {
        TsRecommendEtlTaskVo target = new TsRecommendEtlTaskVo();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /** 将执行实体转换为展示对象。 */
    public static TsRecommendEtlExecutionVo toExecutionVo(
            TsRecommendEtlExecution source) {
        TsRecommendEtlExecutionVo target = new TsRecommendEtlExecutionVo();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
