package org.jeecg.modules.system.vo.tschatmessage;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsChatMessage;

import java.util.ArrayList;
import java.util.List;
public final class TsChatMessageVoConverter {

    private TsChatMessageVoConverter() {
    }
    public static Page<TsChatMessageVo> fromPage(Page<TsChatMessage> source) {
        Page<TsChatMessageVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsChatMessageVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsChatMessage item : source.getRecords()) {
                records.add(fromEntity(item));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsChatMessageVo fromEntity(TsChatMessage source) {
        if (source == null) {
            return null;
        }
        TsChatMessageVo target = new TsChatMessageVo();
        target.setId(source.getId());
        target.setSessionId(source.getSessionId());
        target.setSenderType(source.getSenderType());
        target.setSenderId(source.getSenderId());
        target.setSenderName(source.getSenderName());
        target.setMessageType(source.getMessageType());
        target.setContentText(source.getContentText());
        target.setContentJson(source.getContentJson());
        target.setReplyToMessageId(source.getReplyToMessageId());
        target.setSeqNo(source.getSeqNo());
        target.setGenerateStatus(source.getGenerateStatus());
        target.setCreatedAt(source.getCreatedAt());
        return target;
    }
}
