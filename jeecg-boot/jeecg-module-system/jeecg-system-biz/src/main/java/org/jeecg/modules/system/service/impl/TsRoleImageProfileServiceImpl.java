package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileQueryDto;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileSaveDto;
import org.jeecg.modules.system.entity.TsRoleImageProfile;
import org.jeecg.modules.system.mapper.TsRoleImageProfileMapper;
import org.jeecg.modules.system.po.tsroleimageprofile.TsRoleImageProfileQueryPo;
import org.jeecg.modules.system.po.tsroleimageprofile.TsRoleImageProfileSavePo;
import org.jeecg.modules.system.service.ITsRoleImageProfileService;
import org.jeecg.modules.system.vo.tsroleimageprofile.TsRoleImageProfileVo;
import org.jeecg.modules.system.vo.tsroleimageprofile.TsRoleImageProfileVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Service
public class TsRoleImageProfileServiceImpl extends ServiceImpl<TsRoleImageProfileMapper, TsRoleImageProfile>
        implements ITsRoleImageProfileService {
    @Override
    public Result<Page<TsRoleImageProfileVo>> pageProfiles(LoginUser user, TsRoleImageProfileQueryDto request) {
        TsRoleImageProfileQueryPo queryPo = TsRoleImageProfileQueryPo.fromRequest(user.getId(), request);
        Page<TsRoleImageProfile> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsRoleImageProfile> pageData = baseMapper.selectProfilePage(page, queryPo);
        return Result.OK(TsRoleImageProfileVoConverter.fromPage(pageData));
    }
    @Override
    public Result<TsRoleImageProfileVo> getProfile(LoginUser user, Long id) {
        TsRoleImageProfile entity = baseMapper.selectVisibleById(id, user.getId());
        if (entity == null) {
            throw new JeecgBootException("角色形象模板不存在或无权限访问");
        }
        return Result.OK(TsRoleImageProfileVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRoleImageProfileVo> addProfile(LoginUser user, TsRoleImageProfileSaveDto request) {
        request.applyCreateDefaults();
        TsRoleImageProfileSavePo savePo = TsRoleImageProfileSavePo.fromRequest(request);

        Date now = new Date();
        TsRoleImageProfile entity = new TsRoleImageProfile();
        savePo.applyTo(entity);
        entity.setOwnerUserId(user.getId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);

        return Result.OK("创建成功", TsRoleImageProfileVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRoleImageProfileVo> editProfile(LoginUser user, Long id, TsRoleImageProfileSaveDto request) {
        TsRoleImageProfile entity = baseMapper.selectOwnedById(id, user.getId());
        if (entity == null) {
            throw new JeecgBootException("角色形象模板不存在或无权限修改");
        }

        TsRoleImageProfileSavePo savePo = TsRoleImageProfileSavePo.fromRequest(request);
        savePo.applyTo(entity);
        entity.setOwnerUserId(user.getId());
        entity.setUpdatedAt(new Date());
        this.updateById(entity);

        return Result.OK("更新成功", TsRoleImageProfileVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteProfile(LoginUser user, Long id) {
        TsRoleImageProfile entity = baseMapper.selectOwnedById(id, user.getId());
        if (entity == null) {
            throw new JeecgBootException("角色形象模板不存在或无权限删除");
        }

        entity.setStatus(0);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
        return Result.OK("删除成功");
    }
}