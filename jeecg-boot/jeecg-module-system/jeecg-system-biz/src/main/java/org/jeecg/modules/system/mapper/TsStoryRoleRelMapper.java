package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsStoryRoleRel;

import java.util.List;
public interface TsStoryRoleRelMapper extends BaseMapper<TsStoryRoleRel> {
    List<TsStoryRoleRel> selectByStoryIds(@Param("storyIds") List<Long> storyIds);
    List<TsStoryRoleRel> selectByStoryId(@Param("storyId") Long storyId);
    int deleteByStoryId(@Param("storyId") Long storyId);
    List<Long> selectBoundRoleIds(@Param("storyId") Long storyId, @Param("roleIds") List<Long> roleIds);
}