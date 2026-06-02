package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.system.entity.TsAiLog;
import org.jeecg.modules.system.vo.tsailog.TsAiLogDetailVo;

public interface ITsAiLogService extends IService<TsAiLog> {

    TsAiLogDetailVo getDetail(Long id);
}
