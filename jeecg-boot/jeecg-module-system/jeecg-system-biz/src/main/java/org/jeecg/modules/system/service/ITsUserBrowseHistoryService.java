package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserbrowsehistory.TsUserBrowseHistoryActionDto;
import org.jeecg.modules.system.dto.tsuserbrowsehistory.TsUserBrowseHistoryQueryDto;
import org.jeecg.modules.system.entity.TsUserBrowseHistory;
import org.jeecg.modules.system.vo.tsuserbrowsehistory.TsUserBrowseHistoryRecordVo;
import org.jeecg.modules.system.vo.tsuserbrowsehistory.TsUserBrowseHistoryVo;

/**
 * 用户浏览记录业务服务。
 */
public interface ITsUserBrowseHistoryService extends IService<TsUserBrowseHistory> {

    /**
     * 分页查询当前用户浏览记录。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 浏览记录分页
     */
    Result<Page<TsUserBrowseHistoryVo>> pageHistory(LoginUser user, TsUserBrowseHistoryQueryDto request);

    /**
     * 记录当前用户浏览在线公开角色或故事。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 更新后的浏览记录
     */
    Result<TsUserBrowseHistoryRecordVo> recordHistory(LoginUser user, TsUserBrowseHistoryActionDto request);

    /**
     * 删除当前用户指定浏览记录。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 删除结果
     */
    Result<?> deleteHistory(LoginUser user, TsUserBrowseHistoryActionDto request);

    /**
     * 清空当前用户全部浏览记录。
     *
     * @param user 当前登录用户
     * @return 清空结果
     */
    Result<?> clearHistory(LoginUser user);
}
