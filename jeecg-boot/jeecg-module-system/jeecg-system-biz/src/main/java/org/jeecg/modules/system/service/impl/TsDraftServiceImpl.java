package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsDraftOwnershipAspect;
import org.jeecg.modules.aop.TsDraftOwnershipAspect.CheckTsDraftOwnership;
import org.jeecg.modules.system.dto.tsdraft.TsDraftQueryDto;
import org.jeecg.modules.system.dto.tsdraft.TsDraftSaveDto;
import org.jeecg.modules.system.entity.TsDraft;
import org.jeecg.modules.system.mapper.TsDraftMapper;
import org.jeecg.modules.system.po.tsdraft.TsDraftQueryPo;
import org.jeecg.modules.system.po.tsdraft.TsDraftSavePo;
import org.jeecg.modules.system.service.ITsDraftService;
import org.jeecg.modules.system.vo.tsdraft.TsDraftDetailVo;
import org.jeecg.modules.system.vo.tsdraft.TsDraftListVo;
import org.jeecg.modules.system.vo.tsdraft.TsDraftVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 统一草稿业务服务实现。
 */
@Service
public class TsDraftServiceImpl extends ServiceImpl<TsDraftMapper, TsDraft> implements ITsDraftService {

    /**
     * 分页查询当前用户草稿及页面状态。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 草稿分页，包含完整页面状态
     */
    @Override
    public Result<Page<TsDraftListVo>> pageDrafts(LoginUser user, TsDraftQueryDto request) {
        TsDraftQueryPo queryPo = TsDraftQueryPo.fromRequest(user.getId(), request);
        Page<TsDraft> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsDraft> pageData = baseMapper.selectDraftPage(page, queryPo);
        return Result.OK(TsDraftVoConverter.fromPage(pageData));
    }

    /**
     * 查询当前用户草稿详情。
     *
     * @param user 当前登录用户
     * @param id 草稿 ID
     * @return 草稿详情
     */
    @Override
    @CheckTsDraftOwnership(message = "草稿不存在或无权限访问")
    public Result<TsDraftDetailVo> getDraft(LoginUser user, Long id) {
        TsDraft entity = TsDraftOwnershipAspect.DRAFT_CONTEXT.get();
        return Result.OK(TsDraftVoConverter.toDetailVo(entity));
    }

    /**
     * 新增当前用户草稿。
     *
     * @param user 当前登录用户
     * @param request 保存参数
     * @return 新增后的草稿详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsDraftDetailVo> addDraft(LoginUser user, TsDraftSaveDto request) {
        TsDraftSavePo savePo = TsDraftSavePo.fromRequest(request);
        Date now = new Date();
        TsDraft entity = new TsDraft();
        savePo.applyTo(entity);
        entity.setUserId(user.getId());
        entity.setStatus(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);
        return Result.OK("创建成功", TsDraftVoConverter.toDetailVo(entity));
    }

    /**
     * 编辑当前用户草稿。
     *
     * @param user 当前登录用户
     * @param id 草稿 ID
     * @param request 保存参数
     * @return 编辑后的草稿详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsDraftOwnership(message = "草稿不存在或无权限修改")
    public Result<TsDraftDetailVo> editDraft(LoginUser user, Long id, TsDraftSaveDto request) {
        TsDraft entity = TsDraftOwnershipAspect.DRAFT_CONTEXT.get();
        TsDraftSavePo savePo = TsDraftSavePo.fromRequest(request);
        savePo.applyTo(entity);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
        return Result.OK("更新成功", TsDraftVoConverter.toDetailVo(entity));
    }

    /**
     * 软删除当前用户草稿。
     *
     * @param user 当前登录用户
     * @param id 草稿 ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsDraftOwnership(message = "草稿不存在或无权限删除")
    public Result<?> deleteDraft(LoginUser user, Long id) {
        TsDraft entity = TsDraftOwnershipAspect.DRAFT_CONTEXT.get();
        entity.setStatus(0);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
        return Result.OK("删除成功");
    }
}
