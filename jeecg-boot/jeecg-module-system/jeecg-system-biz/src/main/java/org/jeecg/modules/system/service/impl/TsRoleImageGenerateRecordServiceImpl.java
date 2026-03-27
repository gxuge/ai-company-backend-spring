package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.aop.TsRoleImageGenerateRecordOwnershipAspect;
import org.jeecg.modules.aop.TsRoleImageGenerateRecordOwnershipAspect.CheckTsRoleImageGenerateRecordOwnership;
import org.jeecg.modules.aop.TsRoleOwnershipAspect.CheckTsRoleOwnership;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsroleimagegeneraterecord.TsRoleImageGenerateRecordQueryDto;
import org.jeecg.modules.system.dto.tsroleimagegeneraterecord.TsRoleImageGenerateRecordSaveDto;
import org.jeecg.modules.system.entity.TsRoleImageGenerateRecord;
import org.jeecg.modules.system.mapper.TsRoleImageGenerateRecordMapper;
import org.jeecg.modules.system.po.tsroleimagegeneraterecord.TsRoleImageGenerateRecordQueryPo;
import org.jeecg.modules.system.po.tsroleimagegeneraterecord.TsRoleImageGenerateRecordSavePo;
import org.jeecg.modules.system.service.ITsRoleImageGenerateRecordService;
import org.jeecg.modules.system.vo.tsroleimagegeneraterecord.TsRoleImageGenerateRecordVo;
import org.jeecg.modules.system.vo.tsroleimagegeneraterecord.TsRoleImageGenerateRecordVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Service
public class TsRoleImageGenerateRecordServiceImpl extends ServiceImpl<TsRoleImageGenerateRecordMapper, TsRoleImageGenerateRecord>
        implements ITsRoleImageGenerateRecordService {
    @Override
    public Result<Page<TsRoleImageGenerateRecordVo>> pageRecords(LoginUser user, TsRoleImageGenerateRecordQueryDto request) {
        TsRoleImageGenerateRecordQueryPo queryPo = TsRoleImageGenerateRecordQueryPo.fromRequest(user.getId(), request);
        Page<TsRoleImageGenerateRecord> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsRoleImageGenerateRecord> pageData = baseMapper.selectRecordPage(page, queryPo);
        return Result.OK(TsRoleImageGenerateRecordVoConverter.fromPage(pageData));
    }
    @Override
    @CheckTsRoleImageGenerateRecordOwnership(message = "生成记录不存在或无权限访问")
    public Result<TsRoleImageGenerateRecordVo> getRecord(LoginUser user, Long id) {
        TsRoleImageGenerateRecord entity = TsRoleImageGenerateRecordOwnershipAspect.RECORD_CONTEXT.get();
        return Result.OK(TsRoleImageGenerateRecordVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsRoleOwnership(message = "角色不存在或无操作权限")
    public Result<TsRoleImageGenerateRecordVo> addRecord(LoginUser user, Long roleId, TsRoleImageGenerateRecordSaveDto request) {
        request.applyCreateDefaults();
        TsRoleImageGenerateRecordSavePo savePo = TsRoleImageGenerateRecordSavePo.fromRequest(request);

        Date now = new Date();
        TsRoleImageGenerateRecord entity = new TsRoleImageGenerateRecord();
        savePo.applyTo(entity);
        entity.setRoleId(roleId);
        entity.setUserId(user.getId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);

        return Result.OK("创建成功", TsRoleImageGenerateRecordVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsRoleImageGenerateRecordOwnership(message = "生成记录不存在或无权限修改")
    public Result<TsRoleImageGenerateRecordVo> editRecord(LoginUser user, Long id, TsRoleImageGenerateRecordSaveDto request) {
        TsRoleImageGenerateRecord entity = TsRoleImageGenerateRecordOwnershipAspect.RECORD_CONTEXT.get();
        TsRoleImageGenerateRecordSavePo savePo = TsRoleImageGenerateRecordSavePo.fromRequest(request);
        savePo.applyTo(entity);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);

        return Result.OK("更新成功", TsRoleImageGenerateRecordVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsRoleImageGenerateRecordOwnership(message = "生成记录不存在或无权限删除")
    public Result<?> deleteRecord(LoginUser user, Long id) {
        TsRoleImageGenerateRecord entity = TsRoleImageGenerateRecordOwnershipAspect.RECORD_CONTEXT.get();
        this.removeById(entity.getId());
        return Result.OK("删除成功");
    }
}
