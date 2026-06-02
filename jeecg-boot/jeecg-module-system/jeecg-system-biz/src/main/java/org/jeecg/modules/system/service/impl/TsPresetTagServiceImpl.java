package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.system.entity.TsPresetTag;
import org.jeecg.modules.system.mapper.TsPresetTagMapper;
import org.jeecg.modules.system.service.ITsPresetTagService;
import org.springframework.stereotype.Service;

/**
 * @Description: 预设与标签关联表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Service
public class TsPresetTagServiceImpl extends ServiceImpl<TsPresetTagMapper, TsPresetTag> implements ITsPresetTagService {
}

