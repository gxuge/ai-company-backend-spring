package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardUserQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityUserTaskQueryDto;
import org.jeecg.modules.system.entity.TsActivityRewardRecord;
import org.jeecg.modules.system.entity.TsActivitySignMilestoneRule;
import org.jeecg.modules.system.entity.TsActivityTask;
import org.jeecg.modules.system.entity.TsActivityTaskRewardRule;
import org.jeecg.modules.system.entity.TsUserSignRecord;
import org.jeecg.modules.system.entity.TsUserTaskProgress;
import org.jeecg.modules.system.vo.tsactivity.TsActivityAdminTaskVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityAdminUserTaskVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardRecordVo;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/** 活动中心聚合查询与原子写入 Mapper。 */
public interface TsActivityQueryMapper {

    /** 查询当前有效任务。 */
    List<TsActivityTask> selectActiveTasks(
            @Param("now") Date now,
            @Param("taskType") String taskType,
            @Param("category") String category,
            @Param("conditionType") String conditionType);

    /** 查询当前有效签到任务。 */
    TsActivityTask selectActiveSignTask(@Param("now") Date now);

    /** 幂等创建用户周期进度。 */
    int insertProgressIgnore(TsUserTaskProgress progress);

    /** 查询用户周期进度。 */
    TsUserTaskProgress selectProgress(
            @Param("userId") String userId,
            @Param("taskId") Long taskId,
            @Param("cycleKey") String cycleKey);

    /** 锁定用户周期进度。 */
    TsUserTaskProgress selectProgressForUpdate(
            @Param("userId") String userId,
            @Param("taskId") Long taskId,
            @Param("cycleKey") String cycleKey);

    /** 原子增加用户任务进度。 */
    int incrementProgress(
            @Param("id") Long id,
            @Param("count") Long count,
            @Param("now") Date now);

    /** 奖励成功后将用户任务进度标记为已领取。 */
    int markRewardClaimed(
            @Param("id") Long id,
            @Param("userId") String userId,
            @Param("taskId") Long taskId,
            @Param("now") Date now);

    /** 幂等记录行为事件。 */
    int insertProgressEventIgnore(
            @Param("userId") String userId,
            @Param("conditionType") String conditionType,
            @Param("bizId") String bizId,
            @Param("count") Long count,
            @Param("now") Date now);

    /** 幂等插入签到记录。 */
    int insertSignIgnore(TsUserSignRecord record);

    /** 查询指定日期签到记录。 */
    TsUserSignRecord selectSignByDate(
            @Param("userId") String userId,
            @Param("signDate") LocalDate signDate);

    /** 查询指定日期以前最近一次签到记录。 */
    TsUserSignRecord selectPreviousSign(
            @Param("userId") String userId,
            @Param("signDate") LocalDate signDate);

    /** 查询启用的签到周期里程碑规则。 */
    TsActivitySignMilestoneRule selectActiveSignMilestoneRule(
            @Param("taskId") Long taskId,
            @Param("milestoneDay") Integer milestoneDay);

    /** 查询签到任务全部启用中的周期里程碑规则。 */
    List<TsActivitySignMilestoneRule> selectActiveSignMilestoneRules(
            @Param("taskId") Long taskId);

    /** 按幂等Key查询奖励记录。 */
    TsActivityRewardRecord selectRewardByIdempotency(
            @Param("userId") String userId,
            @Param("idempotencyKey") String idempotencyKey);

    /** 查询启用的会员奖励加成规则。 */
    TsActivityTaskRewardRule selectRewardRule(
            @Param("taskId") Long taskId,
            @Param("memberLevel") String memberLevel);

    /** 查询用户当前会员计划编码。 */
    String selectCurrentMemberPlanCode(
            @Param("userId") String userId,
            @Param("now") Date now);

    /** 后台分页查询活动任务。 */
    Page<TsActivityAdminTaskVo> selectAdminTaskPage(
            Page<TsActivityAdminTaskVo> page,
            @Param("query") TsActivityTaskQueryDto query);

    /** 后台分页查询用户任务进度。 */
    Page<TsActivityAdminUserTaskVo> selectAdminUserTaskPage(
            Page<TsActivityAdminUserTaskVo> page,
            @Param("query") TsActivityUserTaskQueryDto query);

    /** 后台分页查询活动奖励。 */
    Page<TsActivityRewardRecordVo> selectAdminRewardPage(
            Page<TsActivityRewardRecordVo> page,
            @Param("query") TsActivityRewardQueryDto query);

    /** 用户分页查询自己的奖励记录。 */
    Page<TsActivityRewardRecordVo> selectUserRewardPage(
            Page<TsActivityRewardRecordVo> page,
            @Param("userId") String userId,
            @Param("query") TsActivityRewardUserQueryDto query);
}
