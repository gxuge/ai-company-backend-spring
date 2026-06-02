package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.system.entity.TsTagType;
import org.jeecg.modules.system.mapper.TsTagTypeMapper;
import org.jeecg.modules.system.service.ITsTagTypeService;
import org.springframework.stereotype.Service;

/**
 * @Description: 生成标签类型字典表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Service
public class TsTagTypeServiceImpl extends ServiceImpl<TsTagTypeMapper, TsTagType> implements ITsTagTypeService {
}

