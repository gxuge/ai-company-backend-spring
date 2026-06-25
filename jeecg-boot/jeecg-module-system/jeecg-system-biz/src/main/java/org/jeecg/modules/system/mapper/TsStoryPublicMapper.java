package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicBrowseQueryDto;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicQueryDto;
import org.jeecg.modules.system.entity.TsStoryPublic;
import org.jeecg.modules.system.vo.tsstorypublic.TsStoryPublicBrowseVo;
import org.jeecg.modules.system.vo.tsstorypublic.TsStoryPublicVo;

/**
 * 故事公开记录 Mapper。
 */
public interface TsStoryPublicMapper extends BaseMapper<TsStoryPublic> {
    Page<TsStoryPublicVo> selectManagePage(Page<TsStoryPublicVo> page,
                                           @Param("query") TsStoryPublicQueryDto query);

    TsStoryPublicVo selectManageDetail(@Param("id") Long id);

    Page<TsStoryPublicBrowseVo> selectPublicBrowsePage(Page<TsStoryPublicBrowseVo> page,
                                                       @Param("query") TsStoryPublicBrowseQueryDto query);

    TsStoryPublicBrowseVo selectPublicBrowseDetail(@Param("id") Long id,
                                                   @Param("publicId") Long publicId,
                                                   @Param("channelCode") String channelCode);
}
