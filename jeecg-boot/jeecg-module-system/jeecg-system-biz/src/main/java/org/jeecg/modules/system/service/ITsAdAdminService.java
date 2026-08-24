package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsad.TsAdContentQueryDto;
import org.jeecg.modules.system.dto.tsad.TsAdContentSaveDto;
import org.jeecg.modules.system.dto.tsad.TsAdDeliveryRuleSaveDto;
import org.jeecg.modules.system.dto.tsad.TsAdSlotQueryDto;
import org.jeecg.modules.system.dto.tsad.TsAdSlotSaveDto;
import org.jeecg.modules.system.dto.tsad.TsAdStatsQueryDto;
import org.jeecg.modules.system.vo.tsad.TsAdContentVo;
import org.jeecg.modules.system.vo.tsad.TsAdDeliveryRuleVo;
import org.jeecg.modules.system.vo.tsad.TsAdSlotVo;
import org.jeecg.modules.system.vo.tsad.TsAdStatsVo;

/** 广告运营后台服务。 */
public interface ITsAdAdminService {
    /** 分页查询广告位。 */
    Page<TsAdSlotVo> pageSlots(TsAdSlotQueryDto request);
    /** 查询广告位详情。 */
    TsAdSlotVo getSlot(Long id);
    /** 创建广告位。 */
    Long createSlot(TsAdSlotSaveDto request, String operator);
    /** 更新广告位。 */
    void updateSlot(TsAdSlotSaveDto request, String operator);
    /** 软删除空广告位。 */
    void deleteSlot(Long id, String operator);
    /** 更新广告位状态。 */
    void updateSlotStatus(Long id, String status, String operator);
    /** 分页查询广告内容。 */
    Page<TsAdContentVo> pageContents(TsAdContentQueryDto request);
    /** 查询广告内容详情。 */
    TsAdContentVo getContent(Long id);
    /** 创建广告内容。 */
    Long createContent(TsAdContentSaveDto request, String operator);
    /** 更新广告内容并回到草稿。 */
    void updateContent(TsAdContentSaveDto request, String operator);
    /** 软删除广告内容。 */
    void deleteContent(Long id, String operator);
    /** 发布广告内容。 */
    void publishContent(Long id, String operator);
    /** 下线广告内容。 */
    void offlineContent(Long id, String operator);
    /** 查询广告投放规则。 */
    TsAdDeliveryRuleVo getDeliveryRule(Long contentId);
    /** 保存广告投放规则。 */
    void saveDeliveryRule(TsAdDeliveryRuleSaveDto request, String operator);
    /** 查询广告曝光点击汇总。 */
    TsAdStatsVo getStats(TsAdStatsQueryDto request);
}
