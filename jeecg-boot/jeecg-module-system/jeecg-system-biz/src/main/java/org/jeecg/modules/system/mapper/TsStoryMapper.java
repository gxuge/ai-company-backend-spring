package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.po.tsstory.TsStoryQueryPo;
public interface TsStoryMapper extends BaseMapper<TsStory> {
    Page<TsStory> selectStoryPage(Page<TsStory> page, @Param("query") TsStoryQueryPo query);
    TsStory selectOwned(@Param("id") Long id, @Param("userId") String userId);
    TsStory selectOwnedForUpdate(@Param("id") Long id, @Param("userId") String userId);
}