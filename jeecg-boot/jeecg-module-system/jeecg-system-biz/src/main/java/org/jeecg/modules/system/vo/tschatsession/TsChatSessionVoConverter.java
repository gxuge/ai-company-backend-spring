package org.jeecg.modules.system.vo.tschatsession;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsChatSession;

import java.util.ArrayList;
import java.util.List;
public final class TsChatSessionVoConverter {

    private TsChatSessionVoConverter() {
    }
    public static Page<TsChatSessionVo> fromPage(Page<TsChatSession> source) {
        Page<TsChatSessionVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsChatSessionVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsChatSession item : source.getRecords()) {
                records.add(fromEntity(item));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsChatSessionVo fromEntity(TsChatSession source) {
        if (source == null) {
            return null;
        }
        TsChatSessionVo target = new TsChatSessionVo();
        target.setId(source.getId());
        target.setUserId(source.getUserId());
        target.setSessionType(source.getSessionType());
        target.setSessionTitle(source.getSessionTitle());
        target.setTargetRoleId(source.getTargetRoleId());
        target.setStoryId(source.getStoryId());
        target.setSessionStatus(source.getSessionStatus());
        target.setLastMessageId(source.getLastMessageId());
        target.setLastMessageAt(source.getLastMessageAt());
        target.setExtJson(source.getExtJson());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }
}
