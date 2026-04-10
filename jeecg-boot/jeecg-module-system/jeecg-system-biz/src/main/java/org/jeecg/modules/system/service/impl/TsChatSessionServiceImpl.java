package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsChatSessionOwnershipAspect;
import org.jeecg.modules.aop.TsChatSessionOwnershipAspect.CheckTsChatSessionOwnership;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionQueryDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionSaveDto;
import org.jeecg.modules.system.entity.TsChatSession;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.mapper.TsChatSessionMapper;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.po.tschatsession.TsChatSessionQueryPo;
import org.jeecg.modules.system.po.tschatsession.TsChatSessionSavePo;
import org.jeecg.modules.system.service.ITsChatSessionService;
import org.jeecg.modules.system.vo.tschatsession.TsChatSessionVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatSessionVoConverter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class TsChatSessionServiceImpl extends ServiceImpl<TsChatSessionMapper, TsChatSession>
    implements ITsChatSessionService {

    /** 内置系统角色编码 */
    private static final String SYSTEM_ROLE_CODE = "SYSTEM_ASSISTANT";
    /** 每个用户的默认系统会话幂等键 */
    private static final String DEFAULT_SYSTEM_SESSION_KEY = "DEFAULT_SYSTEM";
    /** 默认系统会话标题 */
    private static final String DEFAULT_SYSTEM_SESSION_TITLE = "与系统对话";
    /** 默认系统会话扩展字段 */
    private static final String DEFAULT_SYSTEM_SESSION_EXT_JSON = "{\"builtIn\":true,\"source\":\"auto-init\"}";

    @Resource
    private TsRoleMapper tsRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureDefaultSystemSession(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        TsChatSession existing = this.getOne(new LambdaQueryWrapper<TsChatSession>()
            .eq(TsChatSession::getUserId, userId)
            .eq(TsChatSession::getSystemSessionKey, DEFAULT_SYSTEM_SESSION_KEY)
            .ne(TsChatSession::getSessionStatus, 0)
            .last("LIMIT 1"));
        if (existing != null) {
            return;
        }

        TsRole systemRole = tsRoleMapper.selectOne(new LambdaQueryWrapper<TsRole>()
            .eq(TsRole::getRoleCode, SYSTEM_ROLE_CODE)
            .last("LIMIT 1"));
        if (systemRole == null) {
            throw new JeecgBootBizTipException("未找到内置系统角色，请先执行数据库迁移脚本");
        }

        TsChatSession entity = new TsChatSession();
        entity.setUserId(userId);
        entity.setSessionType("single");
        entity.setSessionTitle(DEFAULT_SYSTEM_SESSION_TITLE);
        entity.setTargetRoleId(systemRole.getId());
        entity.setSessionStatus(1);
        entity.setSystemSessionKey(DEFAULT_SYSTEM_SESSION_KEY);
        entity.setExtJson(DEFAULT_SYSTEM_SESSION_EXT_JSON);
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        try {
            this.save(entity);
        } catch (DuplicateKeyException ignored) {
            // 并发情况下已由其他请求创建成功，忽略重复键异常即可。
        }
    }

    @Override
    public Result<Page<TsChatSessionVo>> pageSessions(LoginUser user, TsChatSessionQueryDto request) {
        // 列表查询时兜底创建默认系统会话，确保每个用户至少一条会话。
        ensureDefaultSystemSession(user.getId());
        TsChatSessionQueryPo queryPo = TsChatSessionQueryPo.fromRequest(user.getId(), request);
        Page<TsChatSession> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsChatSession> pageData = baseMapper.selectSessionPage(page, queryPo);
        return Result.OK(TsChatSessionVoConverter.fromPage(pageData));
    }

    @Override
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<TsChatSessionVo> getSession(LoginUser user, Long id) {
        TsChatSession record = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();
        return Result.OK(TsChatSessionVoConverter.fromEntity(record));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsChatSessionVo> addSession(LoginUser user, TsChatSessionSaveDto request) {
        request.applyCreateDefaults();

        TsChatSessionSavePo savePo = TsChatSessionSavePo.fromRequest(request);
        TsChatSession entity = new TsChatSession();
        savePo.applyCreateTo(entity);
        entity.setUserId(user.getId());
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        this.save(entity);

        return Result.OK("创建成功", TsChatSessionVoConverter.fromEntity(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<TsChatSessionVo> editSession(LoginUser user, Long id, TsChatSessionSaveDto request) {
        TsChatSession record = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();

        TsChatSessionSavePo savePo = TsChatSessionSavePo.fromRequest(request);
        savePo.applyUpdateTo(record);
        record.setUpdatedAt(new Date());
        this.updateById(record);

        return Result.OK("更新成功", TsChatSessionVoConverter.fromEntity(record));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<?> deleteSession(LoginUser user, Long id) {
        TsChatSession record = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();
        if (DEFAULT_SYSTEM_SESSION_KEY.equals(record.getSystemSessionKey())) {
            return Result.error("系统默认会话不可删除");
        }
        this.removeById(record.getId());
        return Result.OK("删除成功");
    }
}
