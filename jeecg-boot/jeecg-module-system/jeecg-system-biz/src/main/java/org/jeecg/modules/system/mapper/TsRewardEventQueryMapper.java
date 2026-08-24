package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventAdminQueryDto;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventAdminDetailVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventAdminItemVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventSummaryVo;

/** 统一奖励事件后台查询 Mapper。 */
public interface TsRewardEventQueryMapper {

    /** 分页查询奖励事件。 */
    Page<TsRewardEventAdminItemVo> selectAdminPage(
            Page<TsRewardEventAdminItemVo> page,
            @Param("query") TsRewardEventAdminQueryDto query);

    /** 查询奖励事件详情。 */
    TsRewardEventAdminDetailVo selectAdminDetail(@Param("id") Long id);

    /** 汇总奖励事件状态。 */
    TsRewardEventSummaryVo selectAdminSummary(
            @Param("query") TsRewardEventAdminQueryDto query);
}
