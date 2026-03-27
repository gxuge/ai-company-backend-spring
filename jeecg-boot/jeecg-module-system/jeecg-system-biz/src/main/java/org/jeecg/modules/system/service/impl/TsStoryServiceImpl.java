package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsStoryOwnershipAspect;
import org.jeecg.modules.aop.TsStoryOwnershipAspect.CheckTsStoryOwnership;
import org.jeecg.modules.system.dto.tsstory.TsStoryQueryDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryRoleBindingDto;
import org.jeecg.modules.system.dto.tsstory.TsStorySaveDto;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.entity.TsStoryRoleRel;
import org.jeecg.modules.system.entity.TsStoryStat;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsStoryChapterMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.jeecg.modules.system.mapper.TsStoryRoleRelMapper;
import org.jeecg.modules.system.mapper.TsStoryStatMapper;
import org.jeecg.modules.system.po.tsstory.TsStoryQueryPo;
import org.jeecg.modules.system.po.tsstory.TsStorySavePo;
import org.jeecg.modules.system.service.ITsStoryService;
import org.jeecg.modules.system.vo.tsstory.TsStoryVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
@Service
public class TsStoryServiceImpl extends ServiceImpl<TsStoryMapper, TsStory> implements ITsStoryService {

    @Resource
    private TsStoryStatMapper tsStoryStatMapper;

    @Resource
    private TsStoryChapterMapper tsStoryChapterMapper;

    @Resource
    private TsStoryRoleRelMapper tsStoryRoleRelMapper;

    @Resource
    private TsRoleMapper tsRoleMapper;
    @Override
    public Result<Page<TsStoryVo>> pageStories(LoginUser user, TsStoryQueryDto request) {
        String userId = user.getId();

        TsStoryQueryPo queryPo = TsStoryQueryPo.fromRequest(userId, request);
        Page<TsStory> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsStory> pageData = baseMapper.selectStoryPage(page, queryPo);

        Map<Long, TsStoryStat> statMap = new HashMap<>();
        Map<Long, List<TsStoryRoleRel>> roleRelMap = new HashMap<>();

        List<TsStory> stories = pageData.getRecords();
        if (stories != null && !stories.isEmpty()) {
            List<Long> storyIds = new ArrayList<>(stories.size());
            for (TsStory story : stories) {
                if (story != null && story.getId() != null) {
                    storyIds.add(story.getId());
                }
            }
            if (!storyIds.isEmpty()) {
                List<TsStoryStat> statList = tsStoryStatMapper.selectByStoryIds(storyIds);
                if (statList != null && !statList.isEmpty()) {
                    for (TsStoryStat stat : statList) {
                        if (stat != null && stat.getStoryId() != null) {
                            statMap.put(stat.getStoryId(), stat);
                        }
                    }
                }

                List<TsStoryRoleRel> roleRelList = tsStoryRoleRelMapper.selectByStoryIds(storyIds);
                if (roleRelList != null && !roleRelList.isEmpty()) {
                    for (TsStoryRoleRel rel : roleRelList) {
                        if (rel != null && rel.getStoryId() != null) {
                            roleRelMap.computeIfAbsent(rel.getStoryId(), key -> new ArrayList<>()).add(rel);
                        }
                    }
                }
            }
        }

        return Result.OK(TsStoryVoConverter.fromPage(pageData, statMap, roleRelMap));
    }
    @Override
    @CheckTsStoryOwnership(message = "故事不存在或无权限访问")
    public Result<TsStoryVo> getStory(LoginUser user, Long id) {
        TsStory story = TsStoryOwnershipAspect.STORY_CONTEXT.get();
        TsStoryStat stat = tsStoryStatMapper.selectById(story.getId());
        List<TsStoryRoleRel> roleRelList = tsStoryRoleRelMapper.selectByStoryId(story.getId());
        return Result.OK(TsStoryVoConverter.fromEntity(story, stat, roleRelList));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsStoryVo> addStory(LoginUser user, TsStorySaveDto request) {
        String userId = user.getId();

        request.applyCreateDefaults();
        TsStorySavePo savePo = TsStorySavePo.fromRequest(request);
        Date now = new Date();

        TsStory story = new TsStory();
        String storyCode = "STORY_"
                + new SimpleDateFormat("yyyyMMddHHmmss").format(now)
                + "_"
                + ThreadLocalRandom.current().nextInt(1000, 10000);
        savePo.applyForCreate(story, user, userId, storyCode, now);
        this.save(story);

        TsStoryStat stat = new TsStoryStat();
        stat.setStoryId(story.getId());
        stat.setFollowerCount(0L);
        stat.setDialogueCount(0L);
        tsStoryStatMapper.insert(stat);

        List<TsStoryRoleBindingDto> normalizedBindings = request.normalizeRoleBindings();
        if (normalizedBindings != null && !normalizedBindings.isEmpty()) {
            List<Long> roleIds = new ArrayList<>(normalizedBindings.size());
            for (TsStoryRoleBindingDto binding : normalizedBindings) {
                roleIds.add(binding.getRoleId());
            }
            List<Long> ownedRoleIds = tsRoleMapper.selectOwnedIds(roleIds, userId);
            if (ownedRoleIds == null || ownedRoleIds.size() != roleIds.size()) {
                throw new JeecgBootException("故事绑定角色包含无权限数据");
            }

            Date relNow = new Date();
            for (TsStoryRoleBindingDto binding : normalizedBindings) {
                TsStoryRoleRel rel = new TsStoryRoleRel();
                rel.setStoryId(story.getId());
                rel.setRoleId(binding.getRoleId());
                rel.setRoleType(binding.getRoleType());
                rel.setSortNo(binding.getSortNo());
                rel.setIsRequired(binding.getIsRequired());
                rel.setJoinSource(binding.getJoinSource());
                rel.setCreatedAt(relNow);
                rel.setUpdatedAt(relNow);
                tsStoryRoleRelMapper.insert(rel);
            }
        }

        List<TsStoryRoleRel> roleRelList = tsStoryRoleRelMapper.selectByStoryId(story.getId());
        return Result.OK("创建成功", TsStoryVoConverter.fromEntity(story, stat, roleRelList));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsStoryOwnership(message = "故事不存在或无权限修改")
    public Result<TsStoryVo> editStory(LoginUser user, Long id, TsStorySaveDto request) {
        String userId = user.getId();

        TsStory story = TsStoryOwnershipAspect.STORY_CONTEXT.get();
        TsStorySavePo savePo = TsStorySavePo.fromRequest(request);
        savePo.applyForUpdate(story, user, userId, new Date());
        this.updateById(story);

        if (request.getRoleBindings() != null) {
            List<TsStoryRoleBindingDto> normalizedBindings = request.normalizeRoleBindings();
            tsStoryRoleRelMapper.deleteByStoryId(story.getId());
            if (normalizedBindings != null && !normalizedBindings.isEmpty()) {
                List<Long> roleIds = new ArrayList<>(normalizedBindings.size());
                for (TsStoryRoleBindingDto binding : normalizedBindings) {
                    roleIds.add(binding.getRoleId());
                }
                List<Long> ownedRoleIds = tsRoleMapper.selectOwnedIds(roleIds, userId);
                if (ownedRoleIds == null || ownedRoleIds.size() != roleIds.size()) {
                    throw new JeecgBootException("故事绑定角色包含无权限数据");
                }

                Date relNow = new Date();
                for (TsStoryRoleBindingDto binding : normalizedBindings) {
                    TsStoryRoleRel rel = new TsStoryRoleRel();
                    rel.setStoryId(story.getId());
                    rel.setRoleId(binding.getRoleId());
                    rel.setRoleType(binding.getRoleType());
                    rel.setSortNo(binding.getSortNo());
                    rel.setIsRequired(binding.getIsRequired());
                    rel.setJoinSource(binding.getJoinSource());
                    rel.setCreatedAt(relNow);
                    rel.setUpdatedAt(relNow);
                    tsStoryRoleRelMapper.insert(rel);
                }
            }
        }

        TsStoryStat stat = tsStoryStatMapper.selectById(story.getId());
        List<TsStoryRoleRel> roleRelList = tsStoryRoleRelMapper.selectByStoryId(story.getId());
        return Result.OK("更新成功", TsStoryVoConverter.fromEntity(story, stat, roleRelList));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsStoryOwnership(message = "故事不存在或无权限删除")
    public Result<?> deleteStory(LoginUser user, Long id) {
        String userId = user.getId();

        TsStory story = TsStoryOwnershipAspect.STORY_CONTEXT.get();
        Date now = new Date();
        story.setIsDeleted(1);
        story.setStatus(9);
        story.setUpdatedBy(userId);
        story.setUpdatedName(user.getRealname());
        story.setUpdatedAt(now);
        this.updateById(story);

        tsStoryChapterMapper.logicDeleteByStoryId(story.getId());
        tsStoryRoleRelMapper.deleteByStoryId(story.getId());
        return Result.OK("删除成功");
    }
}
