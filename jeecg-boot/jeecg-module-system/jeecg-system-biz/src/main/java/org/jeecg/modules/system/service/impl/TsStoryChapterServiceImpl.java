package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsStoryChapterOwnershipAspect;
import org.jeecg.modules.aop.TsStoryChapterOwnershipAspect.CheckTsStoryChapterOwnership;
import org.jeecg.modules.aop.TsStoryOwnershipAspect.CheckTsStoryOwnership;
import org.jeecg.modules.system.dto.tsstorychapter.TsStoryChapterQueryDto;
import org.jeecg.modules.system.dto.tsstorychapter.TsStoryChapterSaveDto;
import org.jeecg.modules.system.entity.TsStoryChapter;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsStoryChapterMapper;
import org.jeecg.modules.system.mapper.TsStoryRoleRelMapper;
import org.jeecg.modules.system.po.tsstorychapter.TsStoryChapterQueryPo;
import org.jeecg.modules.system.po.tsstorychapter.TsStoryChapterSavePo;
import org.jeecg.modules.system.service.ITsStoryChapterService;
import org.jeecg.modules.system.vo.tsstorychapter.TsStoryChapterVo;
import org.jeecg.modules.system.vo.tsstorychapter.TsStoryChapterVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TsStoryChapterServiceImpl extends ServiceImpl<TsStoryChapterMapper, TsStoryChapter>
        implements ITsStoryChapterService {

    @Resource
    private TsRoleMapper tsRoleMapper;

    @Resource
    private TsStoryRoleRelMapper tsStoryRoleRelMapper;

    @Override
    @CheckTsStoryOwnership(message = "故事不存在或无权限访问")
    public Result<Page<TsStoryChapterVo>> pageChapters(LoginUser user, TsStoryChapterQueryDto request) {
        String userId = user.getId();

        TsStoryChapterQueryPo queryPo = TsStoryChapterQueryPo.fromRequest(userId, request);
        Page<TsStoryChapter> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsStoryChapter> pageData = baseMapper.selectChapterPage(page, queryPo);

        Map<Long, List<Long>> forbiddenRoleMap = new HashMap<>();
        List<TsStoryChapter> chapters = pageData.getRecords();
        if (chapters != null && !chapters.isEmpty()) {
            List<Long> chapterIds = new ArrayList<>(chapters.size());
            for (TsStoryChapter chapter : chapters) {
                if (chapter != null && chapter.getId() != null) {
                    chapterIds.add(chapter.getId());
                }
            }
            if (!chapterIds.isEmpty()) {
                List<Map<String, Object>> pairs = baseMapper.selectForbiddenRolePairs(chapterIds);
                if (pairs != null && !pairs.isEmpty()) {
                    for (Map<String, Object> pair : pairs) {
                        Object chapterIdObj = pair.get("chapterId");
                        Object roleIdObj = pair.get("roleId");
                        Long chapterId = chapterIdObj instanceof Number
                                ? ((Number) chapterIdObj).longValue()
                                : (chapterIdObj == null ? null : Long.parseLong(String.valueOf(chapterIdObj)));
                        Long roleId = roleIdObj instanceof Number
                                ? ((Number) roleIdObj).longValue()
                                : (roleIdObj == null ? null : Long.parseLong(String.valueOf(roleIdObj)));
                        if (chapterId != null && roleId != null) {
                            forbiddenRoleMap.computeIfAbsent(chapterId, k -> new ArrayList<>()).add(roleId);
                        }
                    }
                }
            }
        }

        return Result.OK(TsStoryChapterVoConverter.fromPage(pageData, forbiddenRoleMap));
    }

    @Override
    @CheckTsStoryChapterOwnership(message = "章节不存在或无权限访问")
    public Result<TsStoryChapterVo> getChapter(LoginUser user, Long id) {
        TsStoryChapter chapter = TsStoryChapterOwnershipAspect.CHAPTER_CONTEXT.get();
        List<Long> forbiddenRoleIds = baseMapper.selectForbiddenRoleIds(chapter.getId());
        return Result.OK(TsStoryChapterVoConverter.fromEntity(chapter, forbiddenRoleIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsStoryOwnership(message = "故事不存在或无权限访问")
    public Result<TsStoryChapterVo> addChapter(LoginUser user, TsStoryChapterSaveDto request) {
        String userId = user.getId();
        Long storyId = request.getStoryId();

        request.applyCreateDefaults();
        TsStoryChapterSavePo savePo = TsStoryChapterSavePo.fromRequest(request);
        savePo.setStoryId(storyId);

        if (savePo.getChapterNo() == null) {
            Integer maxNo = baseMapper.selectMaxChapterNo(storyId);
            savePo.setChapterNo((maxNo == null ? 0 : maxNo) + 1);
        }
        int duplicateCount = baseMapper.countChapterNo(storyId, savePo.getChapterNo(), null);
        if (duplicateCount > 0) {
            throw new JeecgBootException("章节编号已存在，请修改 chapterNo");
        }

        Long openingRoleId = request.getOpeningRoleId();
        if (openingRoleId != null && openingRoleId <= 0) {
            throw new JeecgBootException("openingRoleId 参数非法");
        }
        List<Long> normalizedForbiddenRoleIds = request.normalizeForbiddenRoleIds();
        Set<Long> candidateRoleSet = new HashSet<>(normalizedForbiddenRoleIds);
        if (openingRoleId != null) {
            candidateRoleSet.add(openingRoleId);
        }
        List<Long> candidateRoleIds = new ArrayList<>(candidateRoleSet);
        if (!candidateRoleIds.isEmpty()) {
            List<Long> ownedRoleIds = tsRoleMapper.selectOwnedIds(candidateRoleIds, userId);
            if (ownedRoleIds == null || ownedRoleIds.size() != candidateRoleIds.size()) {
                throw new JeecgBootException("角色存在无权限数据");
            }
            List<Long> boundRoleIds = tsStoryRoleRelMapper.selectBoundRoleIds(storyId, candidateRoleIds);
            if (boundRoleIds == null || boundRoleIds.size() != candidateRoleIds.size()) {
                throw new JeecgBootException("角色未绑定到当前故事");
            }
        }

        TsStoryChapter chapter = new TsStoryChapter();
        savePo.applyForCreate(chapter, new Date());
        this.save(chapter);

        baseMapper.deleteForbiddenRoles(chapter.getId());
        for (Long roleId : normalizedForbiddenRoleIds) {
            baseMapper.insertForbiddenRole(storyId, chapter.getId(), roleId);
        }

        List<Long> forbiddenRoleIds = baseMapper.selectForbiddenRoleIds(chapter.getId());
        return Result.OK("创建成功", TsStoryChapterVoConverter.fromEntity(chapter, forbiddenRoleIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsStoryChapterOwnership(message = "章节不存在或无权限修改")
    public Result<TsStoryChapterVo> editChapter(LoginUser user, Long id, TsStoryChapterSaveDto request) {
        String userId = user.getId();

        TsStoryChapter chapter = TsStoryChapterOwnershipAspect.CHAPTER_CONTEXT.get();
        Long storyId = request.getStoryId();
        if (!storyId.equals(chapter.getStoryId())) {
            throw new JeecgBootException("storyId 与章节归属不一致");
        }

        TsStoryChapterSavePo savePo = TsStoryChapterSavePo.fromRequest(request);
        savePo.setStoryId(storyId);
        if (savePo.getChapterNo() == null) {
            savePo.setChapterNo(chapter.getChapterNo());
        }
        int duplicateCount = baseMapper.countChapterNo(storyId, savePo.getChapterNo(), chapter.getId());
        if (duplicateCount > 0) {
            throw new JeecgBootException("章节编号已存在，请修改 chapterNo");
        }

        Long openingRoleId = request.getOpeningRoleId();
        if (openingRoleId != null && openingRoleId <= 0) {
            throw new JeecgBootException("openingRoleId 参数非法");
        }
        List<Long> normalizedForbiddenRoleIds = request.normalizeForbiddenRoleIds();
        Set<Long> candidateRoleSet = new HashSet<>(normalizedForbiddenRoleIds);
        if (openingRoleId != null) {
            candidateRoleSet.add(openingRoleId);
        }
        List<Long> candidateRoleIds = new ArrayList<>(candidateRoleSet);
        if (!candidateRoleIds.isEmpty()) {
            List<Long> ownedRoleIds = tsRoleMapper.selectOwnedIds(candidateRoleIds, userId);
            if (ownedRoleIds == null || ownedRoleIds.size() != candidateRoleIds.size()) {
                throw new JeecgBootException("角色存在无权限数据");
            }
            List<Long> boundRoleIds = tsStoryRoleRelMapper.selectBoundRoleIds(storyId, candidateRoleIds);
            if (boundRoleIds == null || boundRoleIds.size() != candidateRoleIds.size()) {
                throw new JeecgBootException("角色未绑定到当前故事");
            }
        }

        savePo.applyForUpdate(chapter, new Date());
        this.updateById(chapter);

        baseMapper.deleteForbiddenRoles(chapter.getId());
        for (Long roleId : normalizedForbiddenRoleIds) {
            baseMapper.insertForbiddenRole(storyId, chapter.getId(), roleId);
        }

        List<Long> forbiddenRoleIds = baseMapper.selectForbiddenRoleIds(chapter.getId());
        return Result.OK("更新成功", TsStoryChapterVoConverter.fromEntity(chapter, forbiddenRoleIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsStoryChapterOwnership(message = "章节不存在或无权限删除")
    public Result<?> deleteChapter(LoginUser user, Long id) {
        TsStoryChapter chapter = TsStoryChapterOwnershipAspect.CHAPTER_CONTEXT.get();
        chapter.setStatus(0);
        chapter.setUpdatedAt(new Date());
        this.updateById(chapter);
        return Result.OK("删除成功");
    }
}