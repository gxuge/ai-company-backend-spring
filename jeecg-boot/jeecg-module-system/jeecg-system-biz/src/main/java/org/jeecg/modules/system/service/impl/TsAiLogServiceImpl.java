package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.modules.system.entity.TsAiLog;
import org.jeecg.modules.system.entity.TsAiLogStep;
import org.jeecg.modules.system.mapper.TsAiLogMapper;
import org.jeecg.modules.system.mapper.TsAiLogStepMapper;
import org.jeecg.modules.system.service.ITsAiLogService;
import org.jeecg.modules.system.vo.tsailog.TsAiLogDetailVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TsAiLogServiceImpl extends ServiceImpl<TsAiLogMapper, TsAiLog> implements ITsAiLogService {

    @Resource
    private TsAiLogStepMapper tsAiLogStepMapper;

    @Override
    public TsAiLogDetailVo getDetail(Long id) {
        TsAiLogDetailVo detail = new TsAiLogDetailVo();
        TsAiLog log = getById(id);
        detail.setLog(log);
        if (log == null) {
            detail.setSteps(List.of());
            return detail;
        }
        QueryWrapper<TsAiLogStep> wrapper = new QueryWrapper<>();
        wrapper.eq("log_id", id)
                .orderByAsc("step_no")
                .orderByAsc("create_time")
                .orderByAsc("id");
        detail.setSteps(tsAiLogStepMapper.selectList(wrapper));
        return detail;
    }
}
