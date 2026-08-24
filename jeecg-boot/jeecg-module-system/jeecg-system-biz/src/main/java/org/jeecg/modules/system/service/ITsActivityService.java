package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsactivity.TsActivityProgressDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardUserQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskListQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskReceiveDto;
import org.jeecg.modules.system.vo.tsactivity.TsActivityHomeVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityProgressResultVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardRecordVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivitySignVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityTaskVo;

import java.util.List;

/** 活动中心用户与内部行为服务。 */
public interface ITsActivityService {

    /** 查询活动首页。 */
    TsActivityHomeVo getHome(String userId);

    /** 执行每日签到。 */
    TsActivitySignVo sign(String userId);

    /** 查询当前用户任务。 */
    List<TsActivityTaskVo> listTasks(
            String userId, TsActivityTaskListQueryDto request);

    /** 幂等领取任务奖励。 */
    TsActivityRewardGrantVo receiveTaskReward(
            String userId, TsActivityTaskReceiveDto request);

    /** 分页查询当前用户奖励记录。 */
    Page<TsActivityRewardRecordVo> pageRewards(
            String userId, TsActivityRewardUserQueryDto request);

    /** 幂等处理内部行为进度。 */
    TsActivityProgressResultVo reportProgress(TsActivityProgressDto request);
}
