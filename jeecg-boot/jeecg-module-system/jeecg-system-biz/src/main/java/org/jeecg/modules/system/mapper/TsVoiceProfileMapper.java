package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.po.tsvoiceprofile.TsVoiceProfileQueryPo;
public interface TsVoiceProfileMapper extends BaseMapper<TsVoiceProfile> {
    Page<TsVoiceProfile> selectVoiceProfilePage(Page<TsVoiceProfile> page, @Param("query") TsVoiceProfileQueryPo query);
    TsVoiceProfile selectActiveById(@Param("id") Long id);
    Long countActiveById(@Param("id") Long id);
}
