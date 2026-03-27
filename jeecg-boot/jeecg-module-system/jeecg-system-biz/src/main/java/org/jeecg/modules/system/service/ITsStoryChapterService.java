package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsstorychapter.TsStoryChapterQueryDto;
import org.jeecg.modules.system.dto.tsstorychapter.TsStoryChapterSaveDto;
import org.jeecg.modules.system.entity.TsStoryChapter;
import org.jeecg.modules.system.vo.tsstorychapter.TsStoryChapterVo;
public interface ITsStoryChapterService extends IService<TsStoryChapter> {
    Result<Page<TsStoryChapterVo>> pageChapters(LoginUser user, TsStoryChapterQueryDto request);
    Result<TsStoryChapterVo> getChapter(LoginUser user, Long id);
    Result<TsStoryChapterVo> addChapter(LoginUser user, TsStoryChapterSaveDto request);
    Result<TsStoryChapterVo> editChapter(LoginUser user, Long id, TsStoryChapterSaveDto request);
    Result<?> deleteChapter(LoginUser user, Long id);
}
