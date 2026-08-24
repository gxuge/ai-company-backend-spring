package org.jeecg.modules.system.service;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsad.TsAdEventReportDto;
import org.jeecg.modules.system.vo.tsad.TsAdSlotDeliveryVo;

import java.util.List;

/** 前端广告投放服务。 */
public interface ITsAdDeliveryService {
    /** 按广告位批量查询当前用户可见内容。 */
    List<TsAdSlotDeliveryVo> deliver(
            List<String> slotCodes, String platform, LoginUser loginUser);
    /** 幂等记录曝光或点击事件，返回本次是否首次接受。 */
    boolean reportEvent(TsAdEventReportDto request, LoginUser loginUser);
}
