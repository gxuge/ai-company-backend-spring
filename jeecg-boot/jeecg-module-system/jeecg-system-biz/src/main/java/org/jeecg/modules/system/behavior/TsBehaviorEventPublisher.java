package org.jeecg.modules.system.behavior;

import org.jeecg.modules.system.event.TsBehaviorEventMessage;

/** 推荐行为消息发布器。 */
public interface TsBehaviorEventPublisher {

    /** 异步发布单条行为事件。 */
    void publish(TsBehaviorEventMessage event);
}
