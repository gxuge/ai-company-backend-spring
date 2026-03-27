package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsStoryChapter;
import org.jeecg.modules.system.po.tsstorychapter.TsStoryChapterQueryPo;

import java.util.List;
import java.util.Map;
public interface TsStoryChapterMapper extends BaseMapper<TsStoryChapter> {
    Page<TsStoryChapter> selectChapterPage(Page<TsStoryChapter> page, @Param("query") TsStoryChapterQueryPo query);
    TsStoryChapter selectOwnedChapter(@Param("id") Long id, @Param("userId") String userId);
    Integer selectMaxChapterNo(@Param("storyId") Long storyId);
    int countChapterNo(@Param("storyId") Long storyId, @Param("chapterNo") Integer chapterNo, @Param("excludeId") Long excludeId);
    List<Long> selectForbiddenRoleIds(@Param("chapterId") Long chapterId);
    List<Map<String, Object>> selectForbiddenRolePairs(@Param("chapterIds") List<Long> chapterIds);
    int deleteForbiddenRoles(@Param("chapterId") Long chapterId);
    int insertForbiddenRole(@Param("storyId") Long storyId, @Param("chapterId") Long chapterId, @Param("roleId") Long roleId);
    int logicDeleteByStoryId(@Param("storyId") Long storyId);
}
