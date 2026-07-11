package org.jeecg.modules.airag.kb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.kb.dto.KbBaseCreateDto;
import org.jeecg.modules.airag.kb.dto.KbBaseQueryDto;
import org.jeecg.modules.airag.kb.dto.KbBaseUpdateDto;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.vo.KbBaseVo;

/**
 * 知识库主表服务。
 */
public interface IKbBaseService extends IService<KbBase> {
    /**
     * 创建知识库并自动创建默认检索配置。
     *
     * @param dto 创建请求
     * @return 知识库返回对象
     */
    KbBaseVo createKb(KbBaseCreateDto dto);

    /**
     * 更新知识库。
     *
     * @param id 知识库ID
     * @param dto 更新请求
     * @return 知识库返回对象
     */
    KbBaseVo updateKb(String id, KbBaseUpdateDto dto);

    /**
     * 删除知识库并级联禁用子数据。
     *
     * @param id 知识库ID
     */
    void deleteKb(String id);

    /**
     * 查询知识库详情。
     *
     * @param id 知识库ID
     * @return 知识库返回对象
     */
    KbBaseVo getKb(String id);

    /**
     * 分页查询知识库列表。
     *
     * @param query 查询请求
     * @return 分页结果
     */
    IPage<KbBaseVo> listKb(KbBaseQueryDto query);
}
