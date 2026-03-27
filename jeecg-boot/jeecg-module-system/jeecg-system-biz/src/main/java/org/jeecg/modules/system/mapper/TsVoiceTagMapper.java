package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsVoiceTag;
import org.jeecg.modules.system.po.tsvoicetag.TsVoiceTagQueryPo;

import java.util.List;
public interface TsVoiceTagMapper extends BaseMapper<TsVoiceTag> {
    Page<TsVoiceTag> selectTagPage(Page<TsVoiceTag> page, @Param("query") TsVoiceTagQueryPo query);
    TsVoiceTag selectByTagId(@Param("id") Long id);
    Long countByTagName(@Param("tagName") String tagName);
    Long countByIds(@Param("tagIds") List<Long> tagIds);
    Long countProfileRelByTagId(@Param("tagId") Long tagId);
}