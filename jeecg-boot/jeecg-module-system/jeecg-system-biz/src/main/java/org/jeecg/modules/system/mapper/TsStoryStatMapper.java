package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.system.entity.TsStoryStat;

import java.util.List;
public interface TsStoryStatMapper extends BaseMapper<TsStoryStat> {
    @Select({
            "<script>",
            "SELECT story_id, follower_count, dialogue_count, updated_at",
            "FROM ts_story_stat",
            "WHERE story_id IN",
            "<foreach collection='storyIds' item='storyId' open='(' separator=',' close=')'>",
            "#{storyId}",
            "</foreach>",
            "</script>"
    })
    List<TsStoryStat> selectByStoryIds(@Param("storyIds") List<Long> storyIds);
}
