package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.system.entity.TsPreset;
import org.jeecg.modules.system.mapper.TsPresetMapper;
import org.jeecg.modules.system.service.ITsPresetService;
import org.springframework.stereotype.Service;

/**
 * @Description: 生成预设主表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Service
public class TsPresetServiceImpl extends ServiceImpl<TsPresetMapper, TsPreset> implements ITsPresetService {
}

