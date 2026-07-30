package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsdraft.TsDraftQueryDto;
import org.jeecg.modules.system.dto.tsdraft.TsDraftSaveDto;
import org.jeecg.modules.system.entity.TsDraft;
import org.jeecg.modules.system.vo.tsdraft.TsDraftDetailVo;
import org.jeecg.modules.system.vo.tsdraft.TsDraftListVo;

/**
 * 统一草稿业务服务。
 */
public interface ITsDraftService extends IService<TsDraft> {

    /**
     * 分页查询当前用户草稿及页面状态。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 草稿分页，包含完整页面状态
     */
    Result<Page<TsDraftListVo>> pageDrafts(LoginUser user, TsDraftQueryDto request);

    /**
     * 查询当前用户草稿详情。
     *
     * @param user 当前登录用户
     * @param id 草稿 ID
     * @return 草稿详情
     */
    Result<TsDraftDetailVo> getDraft(LoginUser user, Long id);

    /**
     * 新增当前用户草稿。
     *
     * @param user 当前登录用户
     * @param request 保存参数
     * @return 新增后的草稿详情
     */
    Result<TsDraftDetailVo> addDraft(LoginUser user, TsDraftSaveDto request);

    /**
     * 编辑当前用户草稿。
     *
     * @param user 当前登录用户
     * @param id 草稿 ID
     * @param request 保存参数
     * @return 编辑后的草稿详情
     */
    Result<TsDraftDetailVo> editDraft(LoginUser user, Long id, TsDraftSaveDto request);

    /**
     * 软删除当前用户草稿。
     *
     * @param user 当前登录用户
     * @param id 草稿 ID
     * @return 删除结果
     */
    Result<?> deleteDraft(LoginUser user, Long id);
}
