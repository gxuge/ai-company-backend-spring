package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsVoiceProfileTag;

import java.util.List;
public interface TsVoiceProfileTagMapper extends BaseMapper<TsVoiceProfileTag> {
    List<TsVoiceProfileTag> selectByProfileIds(@Param("voiceProfileIds") List<Long> voiceProfileIds);
    List<TsVoiceProfileTag> selectByVoiceProfileIds(@Param("voiceProfileIds") List<Long> voiceProfileIds);
    List<Long> selectTagIdsByProfileId(@Param("voiceProfileId") Long voiceProfileId);
    int deleteByProfileId(@Param("voiceProfileId") Long voiceProfileId);
    Integer deleteByVoiceProfileId(@Param("voiceProfileId") Long voiceProfileId);
    Integer insertBatch(@Param("voiceProfileId") Long voiceProfileId, @Param("tagIds") List<Long> tagIds);
}
