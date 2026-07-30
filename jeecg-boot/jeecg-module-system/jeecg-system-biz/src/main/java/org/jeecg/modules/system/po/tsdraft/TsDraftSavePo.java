package org.jeecg.modules.system.po.tsdraft;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.jeecg.modules.system.dto.tsdraft.TsDraftSaveDto;
import org.jeecg.modules.system.entity.TsDraft;

/**
 * 统一草稿保存持久化参数。
 */
@Data
public class TsDraftSavePo {

    /** 草稿类型：role 角色，story 故事。 */
    private String draftType;

    /** 草稿箱展示名称。 */
    private String draftName;

    /** 来源正式资源 ID。 */
    private Long sourceId;

    /** 页面完整状态 JSON。 */
    private String contentJson;

    /**
     * 将接口保存参数转换为持久化参数。
     *
     * @param request 保存参数
     * @return 持久化参数
     */
    public static TsDraftSavePo fromRequest(TsDraftSaveDto request) {
        TsDraftSavePo po = new TsDraftSavePo();
        if (request == null) {
            return po;
        }
        po.setDraftType(trimToNull(request.getDraftType()));
        po.setDraftName(trimToNull(request.getDraftName()));
        po.setSourceId(request.getSourceId());
        po.setContentJson(JSON.toJSONString(request.getContent()));
        return po;
    }

    /**
     * 将保存参数应用到草稿实体。
     *
     * @param entity 草稿实体
     */
    public void applyTo(TsDraft entity) {
        if (entity == null) {
            return;
        }
        entity.setDraftType(this.draftType);
        entity.setDraftName(this.draftName);
        entity.setSourceId(this.sourceId);
        entity.setContentJson(this.contentJson);
    }

    /**
     * 去除字符串首尾空白并将空字符串转换为 null。
     *
     * @param value 原始字符串
     * @return 归一化字符串
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
