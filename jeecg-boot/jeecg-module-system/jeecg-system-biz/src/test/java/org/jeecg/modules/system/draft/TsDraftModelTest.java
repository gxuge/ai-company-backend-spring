package org.jeecg.modules.system.draft;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsdraft.TsDraftQueryDto;
import org.jeecg.modules.system.dto.tsdraft.TsDraftSaveDto;
import org.jeecg.modules.system.entity.TsDraft;
import org.jeecg.modules.system.po.tsdraft.TsDraftQueryPo;
import org.jeecg.modules.system.po.tsdraft.TsDraftSavePo;
import org.jeecg.modules.system.vo.tsdraft.TsDraftDetailVo;
import org.jeecg.modules.system.vo.tsdraft.TsDraftListVo;
import org.jeecg.modules.system.vo.tsdraft.TsDraftVoConverter;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 统一草稿模型转换测试。
 */
class TsDraftModelTest {

    /**
     * 验证草稿 JSON 保存与详情恢复保持结构一致。
     */
    @Test
    void shouldRoundTripStructuredDraftContent() {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("roleName", "林墨");
        content.put("gender", "male");

        TsDraftSaveDto request = new TsDraftSaveDto();
        request.setDraftType(" role ");
        request.setDraftName(" 角色草稿 ");
        request.setContent(content);

        TsDraftSavePo savePo = TsDraftSavePo.fromRequest(request);
        TsDraft entity = new TsDraft();
        savePo.applyTo(entity);
        entity.setId(1L);
        entity.setStatus(1);

        TsDraftDetailVo detail = TsDraftVoConverter.toDetailVo(entity);

        assertEquals("role", detail.getDraftType());
        assertEquals("角色草稿", detail.getDraftName());
        assertEquals("林墨", detail.getContent().get("roleName"));
        assertEquals("male", detail.getContent().get("gender"));
    }

    /**
     * 验证分页参数上限和查询字符串归一化。
     */
    @Test
    void shouldNormalizeDraftPageQuery() {
        TsDraftQueryDto request = new TsDraftQueryDto();
        request.setPageNo(0);
        request.setPageSize(500);
        request.setKeyword("  章节  ");
        request.setDraftType(" story ");

        TsDraftQueryPo queryPo = TsDraftQueryPo.fromRequest("10001", request);

        assertEquals(1, queryPo.getPageNo());
        assertEquals(100, queryPo.getPageSize());
        assertEquals("章节", queryPo.getKeyword());
        assertEquals("story", queryPo.getDraftType());
    }

    /**
     * 验证非法数据库 JSON 不会直接泄漏字符串或抛出转换异常。
     */
    @Test
    void shouldFallbackToEmptyContentForInvalidJson() {
        TsDraft entity = new TsDraft();
        entity.setContentJson("{invalid");

        TsDraftDetailVo detail = TsDraftVoConverter.toDetailVo(entity);

        assertFalse(detail.getContent().containsKey("invalid"));
        assertEquals(0, detail.getContent().size());
    }

    /**
     * 验证列表响应也能返回结构化页面状态。
     */
    @Test
    void shouldExposeStructuredContentInDraftList() {
        TsDraft entity = new TsDraft();
        entity.setContentJson("{\"name\":\"林墨\",\"avatarUrl\":\"https://example.com/avatar.png\"}");
        Page<TsDraft> source = new Page<>(1, 20, 1);
        source.setRecords(List.of(entity));

        Page<TsDraftListVo> result = TsDraftVoConverter.fromPage(source);

        assertEquals("林墨", result.getRecords().get(0).getContent().get("name"));
        assertEquals("https://example.com/avatar.png", result.getRecords().get(0).getContent().get("avatarUrl"));
    }
}
