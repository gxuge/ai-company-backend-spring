package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.dto.tsad.TsAdContentQueryDto;
import org.jeecg.modules.system.dto.tsad.TsAdSlotQueryDto;
import org.jeecg.modules.system.dto.tsad.TsAdStatsQueryDto;
import org.jeecg.modules.system.entity.TsAdEvent;
import org.jeecg.modules.system.po.tsad.TsAdDeliveryCandidatePo;
import org.jeecg.modules.system.vo.tsad.TsAdContentVo;
import org.jeecg.modules.system.vo.tsad.TsAdSlotVo;
import org.jeecg.modules.system.vo.tsad.TsAdStatsVo;

import java.util.Date;
import java.util.List;

/** 广告投放聚合查询与幂等写入 Mapper。 */
public interface TsAdQueryMapper {

    /** 后台分页查询广告位。 */
    Page<TsAdSlotVo> selectSlotPage(
            Page<TsAdSlotVo> page,
            @Param("query") TsAdSlotQueryDto query);

    /** 查询广告位详情。 */
    TsAdSlotVo selectSlotDetail(@Param("id") Long id);

    /** 后台分页查询广告内容。 */
    Page<TsAdContentVo> selectContentPage(
            Page<TsAdContentVo> page,
            @Param("query") TsAdContentQueryDto query);

    /** 查询广告内容详情。 */
    TsAdContentVo selectContentDetail(@Param("id") Long id);

    /** 批量查询当前可投放候选内容，服务层继续执行受众规则过滤。 */
    List<TsAdDeliveryCandidatePo> selectDeliveryCandidates(
            @Param("slotCodes") List<String> slotCodes,
            @Param("now") Date now);

    /** 幂等插入曝光或点击事件。 */
    int insertEventIgnore(TsAdEvent event);

    /** 聚合查询曝光、点击与点击率。 */
    TsAdStatsVo selectStats(@Param("query") TsAdStatsQueryDto query);
}
