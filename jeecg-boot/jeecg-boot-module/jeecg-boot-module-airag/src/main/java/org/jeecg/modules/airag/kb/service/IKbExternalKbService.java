package org.jeecg.modules.airag.kb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.kb.dto.KbExternalKbQueryDto;
import org.jeecg.modules.airag.kb.dto.KbExternalKbSaveDto;
import org.jeecg.modules.airag.kb.entity.KbExternalKb;
import org.jeecg.modules.airag.kb.vo.KbExternalKbVo;

/**
 * 外部知识库服务。
 */
public interface IKbExternalKbService extends IService<KbExternalKb> {
    /**
     * 新增外部知识库。
     *
     * @param dto 请求
     * @return 返回对象
     */
    KbExternalKbVo create(KbExternalKbSaveDto dto);

    /**
     * 更新外部知识库。
     *
     * @param id 主键
     * @param dto 请求
     * @return 返回对象
     */
    KbExternalKbVo update(String id, KbExternalKbSaveDto dto);

    /**
     * 删除外部知识库。
     *
     * @param id 主键
     */
    void delete(String id);

    /**
     * 查询详情。
     *
     * @param id 主键
     * @return 返回对象
     */
    KbExternalKbVo getDetail(String id);

    /**
     * 查询列表。
     *
     * @param query 查询条件
     * @return 分页
     */
    IPage<KbExternalKbVo> page(KbExternalKbQueryDto query);

    /**
     * 测试连接。
     *
     * @param id 主键
     * @return 测试结果
     */
    String testConnection(String id);

    /**
     * 通过external_kb_id查询。
     *
     * @param externalKbId 外部知识库ID
     * @return 返回对象
     */
    KbExternalKbVo getByExternalKbId(String externalKbId);

    /**
     * 通过external_kb_id查询实体。
     *
     * @param externalKbId 外部知识库ID
     * @return 实体
     */
    KbExternalKb getEntityByExternalKbId(String externalKbId);
}
