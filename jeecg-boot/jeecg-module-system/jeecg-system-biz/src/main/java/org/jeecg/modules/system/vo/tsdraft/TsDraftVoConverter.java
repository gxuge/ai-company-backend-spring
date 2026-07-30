package org.jeecg.modules.system.vo.tsdraft;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsDraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一草稿展示对象转换器。
 */
public final class TsDraftVoConverter {

    private TsDraftVoConverter() {
    }

    /**
     * 转换草稿分页摘要。
     *
     * @param source 草稿实体分页
     * @return 草稿分页，包含完整页面状态
     */
    public static Page<TsDraftListVo> fromPage(Page<TsDraft> source) {
        Page<TsDraftListVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsDraftListVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsDraft entity : source.getRecords()) {
                records.add(toListVo(entity));
            }
        }
        target.setRecords(records);
        return target;
    }

    /**
     * 转换草稿详情。
     *
     * @param entity 草稿实体
     * @return 草稿详情
     */
    public static TsDraftDetailVo toDetailVo(TsDraft entity) {
        if (entity == null) {
            return null;
        }
        TsDraftDetailVo vo = new TsDraftDetailVo();
        vo.setId(entity.getId());
        vo.setDraftType(entity.getDraftType());
        vo.setDraftName(entity.getDraftName());
        vo.setSourceId(entity.getSourceId());
        vo.setContent(parseContent(entity.getContentJson()));
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 转换草稿列表对象。
     *
     * @param entity 草稿实体
     * @return 草稿列表对象
     */
    private static TsDraftListVo toListVo(TsDraft entity) {
        TsDraftListVo vo = new TsDraftListVo();
        vo.setId(entity.getId());
        vo.setDraftType(entity.getDraftType());
        vo.setDraftName(entity.getDraftName());
        vo.setSourceId(entity.getSourceId());
        vo.setContent(parseContent(entity.getContentJson()));
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 将数据库 JSON 转换为结构化草稿内容。
     *
     * @param contentJson 草稿 JSON
     * @return 结构化草稿内容
     */
    private static Map<String, Object> parseContent(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> content = JSON.parseObject(
                    contentJson,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    }
            );
            return content == null ? new LinkedHashMap<>() : content;
        } catch (RuntimeException ignored) {
            return new LinkedHashMap<>();
        }
    }
}
