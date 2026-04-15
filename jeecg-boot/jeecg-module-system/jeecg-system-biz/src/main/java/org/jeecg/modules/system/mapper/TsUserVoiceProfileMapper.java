package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsUserVoiceProfile;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.po.tsuservoiceprofile.TsUserVoiceProfileQueryPo;

import java.util.Date;

public interface TsUserVoiceProfileMapper extends BaseMapper<TsUserVoiceProfile> {
    Page<TsVoiceProfile> selectUserVoiceProfilePage(Page<TsVoiceProfile> page, @Param("query") TsUserVoiceProfileQueryPo query);
    TsUserVoiceProfile selectOwnedActive(@Param("voiceProfileId") Long voiceProfileId, @Param("userId") String userId);
    TsVoiceProfile selectOwnedActiveVoiceProfile(@Param("voiceProfileId") Long voiceProfileId, @Param("userId") String userId);
    int insertOrReactivate(@Param("userId") String userId, @Param("voiceProfileId") Long voiceProfileId, @Param("now") Date now);
}

