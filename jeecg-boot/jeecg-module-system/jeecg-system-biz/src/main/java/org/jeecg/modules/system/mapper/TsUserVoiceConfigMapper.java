package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsUserVoiceConfig;
public interface TsUserVoiceConfigMapper extends BaseMapper<TsUserVoiceConfig> {
    TsUserVoiceConfig selectByUserId(@Param("userId") String userId);
    Long countBySelectedVoiceProfileId(@Param("selectedVoiceProfileId") Long selectedVoiceProfileId);
}
