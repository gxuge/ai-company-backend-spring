package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsroleimagegeneraterecord.TsRoleImageGenerateRecordQueryDto;
import org.jeecg.modules.system.dto.tsroleimagegeneraterecord.TsRoleImageGenerateRecordSaveDto;
import org.jeecg.modules.system.entity.TsRoleImageGenerateRecord;
import org.jeecg.modules.system.vo.tsroleimagegeneraterecord.TsRoleImageGenerateRecordVo;
public interface ITsRoleImageGenerateRecordService extends IService<TsRoleImageGenerateRecord> {
    Result<Page<TsRoleImageGenerateRecordVo>> pageRecords(LoginUser user, TsRoleImageGenerateRecordQueryDto request);
    Result<TsRoleImageGenerateRecordVo> getRecord(LoginUser user, Long id);
    Result<TsRoleImageGenerateRecordVo> addRecord(LoginUser user, Long roleId, TsRoleImageGenerateRecordSaveDto request);
    Result<TsRoleImageGenerateRecordVo> editRecord(LoginUser user, Long id, TsRoleImageGenerateRecordSaveDto request);
    Result<?> deleteRecord(LoginUser user, Long id);
}
