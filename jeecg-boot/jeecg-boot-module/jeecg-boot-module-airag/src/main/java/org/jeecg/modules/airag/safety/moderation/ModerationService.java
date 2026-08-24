package org.jeecg.modules.airag.safety.moderation;

/**
 * 统一文本安全审核服务。
 *
 * <p>业务代码只依赖此接口。切换厂商或自部署模型时替换实现即可。</p>
 */
public interface ModerationService {
    /**
     * 审核一段文本。
     *
     * @param request 审核请求
     * @return 统一审核结果
     */
    ModerationResult moderate(ModerationRequest request);

    /**
     * 返回当前审核实现名称。
     *
     * @return 服务名称
     */
    String serviceName();
}
