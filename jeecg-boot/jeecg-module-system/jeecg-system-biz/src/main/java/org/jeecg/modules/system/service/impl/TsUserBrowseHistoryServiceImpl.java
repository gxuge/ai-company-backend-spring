package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserbrowsehistory.TsUserBrowseHistoryActionDto;
import org.jeecg.modules.system.dto.tsuserbrowsehistory.TsUserBrowseHistoryQueryDto;
import org.jeecg.modules.system.entity.TsUserBrowseHistory;
import org.jeecg.modules.system.mapper.TsUserBrowseHistoryMapper;
import org.jeecg.modules.system.po.tsuserbrowsehistory.TsUserBrowseHistoryQueryPo;
import org.jeecg.modules.system.service.ITsUserBrowseHistoryService;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceResolver;
import org.jeecg.modules.system.vo.tsuserbrowsehistory.TsUserBrowseHistoryRecordVo;
import org.jeecg.modules.system.vo.tsuserbrowsehistory.TsUserBrowseHistoryVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 用户浏览记录业务服务实现。
 */
@Service
public class TsUserBrowseHistoryServiceImpl extends ServiceImpl<TsUserBrowseHistoryMapper, TsUserBrowseHistory>
        implements ITsUserBrowseHistoryService {

    /**
     * 分页查询当前用户浏览记录，仅返回仍在线可访问的角色和故事。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 浏览记录分页
     */
    @Override
    public Result<Page<TsUserBrowseHistoryVo>> pageHistory(
            LoginUser user, TsUserBrowseHistoryQueryDto request) {
        TsUserBrowseHistoryQueryPo queryPo = TsUserBrowseHistoryQueryPo.fromRequest(user.getId(), request);
        Page<TsUserBrowseHistoryVo> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsUserBrowseHistoryVo> pageData = baseMapper.selectHistoryPage(page, queryPo);
        enrichImageResources(pageData.getRecords());
        return Result.OK(pageData);
    }

    /**
     * 记录当前用户浏览行为，重复浏览累加次数并更新最近浏览时间。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 更新后的浏览记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserBrowseHistoryRecordVo> recordHistory(
            LoginUser user, TsUserBrowseHistoryActionDto request) {
        if (baseMapper.countAvailableResource(request.getResourceType(), request.getResourceId()) <= 0) {
            throw new JeecgBootException("资源不存在、已下架或不可记录");
        }
        baseMapper.upsertHistory(
                user.getId(), request.getResourceType(), request.getResourceId(), new Date());
        TsUserBrowseHistory history = baseMapper.selectActiveHistory(
                user.getId(), request.getResourceType(), request.getResourceId());
        return Result.OK("浏览记录已更新", TsUserBrowseHistoryRecordVo.fromEntity(history));
    }

    /**
     * 软删除当前用户指定浏览记录，记录不存在时仍返回成功。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteHistory(LoginUser user, TsUserBrowseHistoryActionDto request) {
        baseMapper.deleteHistory(user.getId(), request.getResourceType(), request.getResourceId());
        return Result.OK("删除成功");
    }

    /**
     * 软删除当前用户全部有效浏览记录。
     *
     * @param user 当前登录用户
     * @return 清空结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> clearHistory(LoginUser user) {
        baseMapper.clearHistory(user.getId());
        return Result.OK("清空成功");
    }

    /**
     * 根据资源类型补充统一图片语义。
     *
     * @param records 浏览记录列表
     */
    private void enrichImageResources(List<TsUserBrowseHistoryVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (TsUserBrowseHistoryVo item : records) {
            if ("role".equals(item.getResourceType())) {
                item.setImageResources(TsImageResourceResolver.buildRolePublicBrowseImageResources(
                        item.getResourceId(), item.getAvatarUrl(), item.getCoverUrl(), item.getAuthorAvatar()));
            } else if ("story".equals(item.getResourceType())) {
                item.setImageResources(TsImageResourceResolver.buildStoryPublicBrowseImageResources(
                        item.getResourceId(), item.getSceneImageUrl(), item.getCoverUrl(), item.getAuthorAvatar()));
            }
        }
    }
}
