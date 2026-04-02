package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsChatMessage;
import org.jeecg.modules.system.po.tschatmessage.TsChatMessageQueryPo;

import java.util.List;

public interface TsChatMessageMapper extends BaseMapper<TsChatMessage> {

    /**
     * 分页查询当前用户可访问的会话消息列表。
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 消息分页结果
     */
    Page<TsChatMessage> selectMessagePage(Page<TsChatMessage> page, @Param("query") TsChatMessageQueryPo query);

    /**
     * 查询当前用户可访问的单条消息。
     *
     * @param id 消息 ID
     * @param userId 用户 ID
     * @return 消息实体
     */
    TsChatMessage selectOwnedById(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 统计指定会话是否归属当前用户。
     *
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     * @return 归属记录数
     */
    Integer selectOwnedSessionCount(@Param("sessionId") Long sessionId, @Param("userId") String userId);

    /**
     * 查询并锁定会话下一个消息顺序号。
     *
     * @param sessionId 会话 ID
     * @return 下一个顺序号
     */
    Long selectNextSeqNoForUpdate(@Param("sessionId") Long sessionId);

    /**
     * 查询会话最近消息，用于拼接 AI 上下文。
     *
     * @param sessionId 会话 ID
     * @param limit 记录条数上限
     * @return 最近消息列表（按 seq_no 倒序）
     */
    List<TsChatMessage> selectRecentMessages(@Param("sessionId") Long sessionId, @Param("limit") Integer limit);
}
