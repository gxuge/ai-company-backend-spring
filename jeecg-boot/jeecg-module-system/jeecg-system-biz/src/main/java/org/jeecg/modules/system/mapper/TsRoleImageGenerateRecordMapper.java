package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsRoleImageGenerateRecord;
import org.jeecg.modules.system.po.tsroleimagegeneraterecord.TsRoleImageGenerateRecordQueryPo;
public interface TsRoleImageGenerateRecordMapper extends BaseMapper<TsRoleImageGenerateRecord> {
    Page<TsRoleImageGenerateRecord> selectRecordPage(Page<TsRoleImageGenerateRecord> page,
                                                     @Param("query") TsRoleImageGenerateRecordQueryPo query);
    TsRoleImageGenerateRecord selectOwned(@Param("id") Long id, @Param("userId") String userId);
}
