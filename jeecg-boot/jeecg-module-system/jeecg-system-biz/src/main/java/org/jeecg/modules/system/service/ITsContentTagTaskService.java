package org.jeecg.modules.system.service;

/** 内容标签异步任务服务。 */
public interface ITsContentTagTaskService {

    /** 根据审核快照创建任务，已存在时复用原任务。 */
    Long enqueue(Long reviewId);

    /** 执行一次内容标签任务。 */
    void execute(Long taskId);

    /** 将失败任务恢复为待执行状态并返回任务ID。 */
    Long retry(Long taskId);
}
