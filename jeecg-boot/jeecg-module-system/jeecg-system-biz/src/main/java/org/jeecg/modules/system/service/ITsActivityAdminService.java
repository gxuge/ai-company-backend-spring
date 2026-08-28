package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardRuleSaveDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivitySignMilestoneRuleSaveDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskCreateDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskUpdateDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityUserTaskQueryDto;
import org.jeecg.modules.system.entity.TsActivityTaskRewardRule;
import org.jeecg.modules.system.entity.TsActivitySignMilestoneRule;
import org.jeecg.modules.system.vo.tsactivity.TsActivityAdminTaskVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityAdminUserTaskVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardRecordVo;

import java.util.List;

/** 活动中心后台管理服务。 */
public interface ITsActivityAdminService {

    /** 分页查询任务配置。 */
    Page<TsActivityAdminTaskVo> pageTasks(TsActivityTaskQueryDto request);

    /** 创建任务并返回任务ID。 */
    Long createTask(TsActivityTaskCreateDto request);

    /** 编辑任务。 */
    void updateTask(TsActivityTaskUpdateDto request);

    /** 分页查询用户任务进度。 */
    Page<TsActivityAdminUserTaskVo> pageUserTasks(
            TsActivityUserTaskQueryDto request);

    /** 分页查询奖励记录。 */
    Page<TsActivityRewardRecordVo> pageRewards(
            TsActivityRewardQueryDto request);

    /** 查询会员奖励加成规则。 */
    List<TsActivityTaskRewardRule> listRewardRules();

    /** 保存会员奖励加成规则。 */
    void saveRewardRule(TsActivityRewardRuleSaveDto request);

    /** 查询签到周期里程碑奖励规则。 */
    List<TsActivitySignMilestoneRule> listSignMilestoneRules(Long taskId);

    /** 保存签到周期里程碑奖励规则。 */
    void saveSignMilestoneRule(TsActivitySignMilestoneRuleSaveDto request);
}
