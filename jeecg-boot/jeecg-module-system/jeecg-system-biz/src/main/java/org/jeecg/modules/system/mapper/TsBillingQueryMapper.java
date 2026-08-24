package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.dto.tsbilling.TsBillingQueryDto;
import org.jeecg.modules.system.vo.tsbilling.TsBillingDetailVo;
import org.jeecg.modules.system.vo.tsbilling.TsBillingRecordVo;
import org.jeecg.modules.system.vo.tsbilling.TsBillingSummaryVo;

/** 双视角统一账单查询 Mapper。 */
public interface TsBillingQueryMapper {

    /** 分页查询统一账单。 */
    Page<TsBillingRecordVo> selectBillingPage(
            Page<TsBillingRecordVo> page,
            @Param("query") TsBillingQueryDto query,
            @Param("userId") String userId,
            @Param("platformView") boolean platformView);

    /** 查询统一账单详情。 */
    TsBillingDetailVo selectBillingDetail(
            @Param("recordType") String recordType,
            @Param("recordId") Long recordId,
            @Param("userId") String userId,
            @Param("platformView") boolean platformView);

    /** 汇总平台账单。 */
    TsBillingSummaryVo selectPlatformSummary(@Param("query") TsBillingQueryDto query);
}
