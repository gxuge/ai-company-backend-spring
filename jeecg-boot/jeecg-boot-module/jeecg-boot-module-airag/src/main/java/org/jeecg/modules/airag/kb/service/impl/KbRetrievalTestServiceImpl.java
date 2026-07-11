package org.jeecg.modules.airag.kb.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbRetrievalTestLogQueryDTO;
import org.jeecg.modules.airag.kb.dto.KbSemanticSearchQueryDTO;
import org.jeecg.modules.airag.kb.entity.KbRetrievalTestLog;
import org.jeecg.modules.airag.kb.service.IKbRetrievalTestLogService;
import org.jeecg.modules.airag.kb.service.IKbRetrievalTestService;
import org.jeecg.modules.airag.kb.service.IKbSemanticSearchService;
import org.jeecg.modules.airag.kb.vo.KbRetrievalTestLogVo;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索测试服务实现。
 */
@Service
public class KbRetrievalTestServiceImpl implements IKbRetrievalTestService {
    /**
     * 知识库检索服务。
     */
    private final IKbSemanticSearchService kbSemanticSearchService;

    /**
     * 检索日志服务。
     */
    private final IKbRetrievalTestLogService kbRetrievalTestLogService;

    /**
     * 构造方法。
     *
     * @param kbSemanticSearchService 检索服务
     * @param kbRetrievalTestLogService 日志服务
     */
    public KbRetrievalTestServiceImpl(IKbSemanticSearchService kbSemanticSearchService,
                                      IKbRetrievalTestLogService kbRetrievalTestLogService) {
        this.kbSemanticSearchService = kbSemanticSearchService;
        this.kbRetrievalTestLogService = kbRetrievalTestLogService;
    }

    @Override
    public KbSemanticSearchResultVO testSearch(String kbId, KbSemanticSearchQueryDTO dto) {
        KbRetrievalTestLog log = new KbRetrievalTestLog();
        log.setKbId(kbId);
        log.setQuery(dto == null ? null : dto.getQuery());
        log.setSearchMode(dto == null ? null : dto.getSearchMode());
        log.setCreatedAt(new Date());
        log.setUpdatedAt(log.getCreatedAt());
        try {
            KbSemanticSearchResultVO result = kbSemanticSearchService.search(kbId, dto);
            Map<String, Object> trace = kbSemanticSearchService.consumeLastSearchTrace();
            fillLog(log, dto, result, trace, KbConstants.LOG_STATUS_SUCCESS, null);
            saveLogSafely(log);
            return result;
        } catch (RuntimeException ex) {
            Map<String, Object> trace = kbSemanticSearchService.consumeLastSearchTrace();
            fillLog(log, dto, null, trace, KbConstants.LOG_STATUS_FAILED, ex.getMessage());
            saveLogSafely(log);
            throw ex;
        }
    }

    @Override
    public IPage<KbRetrievalTestLogVo> pageLogs(KbRetrievalTestLogQueryDTO dto) {
        int pageNo = dto == null || dto.getPageNo() == null || dto.getPageNo() < 1 ? 1 : dto.getPageNo();
        int pageSize = dto == null || dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : Math.min(dto.getPageSize(), 100);
        Page<KbRetrievalTestLog> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<KbRetrievalTestLog> wrapper = new LambdaQueryWrapper<>();
        if (dto != null) {
            if (oConvertUtils.isNotEmpty(dto.getKbId())) {
                wrapper.eq(KbRetrievalTestLog::getKbId, dto.getKbId());
            }
            if (oConvertUtils.isNotEmpty(dto.getQuery())) {
                wrapper.like(KbRetrievalTestLog::getQuery, dto.getQuery());
            }
            if (oConvertUtils.isNotEmpty(dto.getStatus())) {
                wrapper.eq(KbRetrievalTestLog::getStatus, dto.getStatus());
            }
            if (dto.getStartTime() != null) {
                wrapper.ge(KbRetrievalTestLog::getCreatedAt, dto.getStartTime());
            }
            if (dto.getEndTime() != null) {
                wrapper.le(KbRetrievalTestLog::getCreatedAt, dto.getEndTime());
            }
        }
        wrapper.orderByDesc(KbRetrievalTestLog::getCreatedAt);
        IPage<KbRetrievalTestLog> pageData = kbRetrievalTestLogService.page(page, wrapper);
        Page<KbRetrievalTestLogVo> voPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        List<KbRetrievalTestLogVo> records = new ArrayList<>();
        for (KbRetrievalTestLog entity : pageData.getRecords()) {
            records.add(KbRetrievalTestLogVo.from(entity));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public KbRetrievalTestLogVo getLogById(String id) {
        KbRetrievalTestLog entity = kbRetrievalTestLogService.getById(id);
        if (entity == null) {
            throw new JeecgBootException("未找到对应检索日志");
        }
        return KbRetrievalTestLogVo.from(entity);
    }

    /**
     * 安全保存日志。
     *
     * @param log 日志
     */
    private void saveLogSafely(KbRetrievalTestLog log) {
        try {
            kbRetrievalTestLogService.save(log);
        } catch (Exception ignored) {
            // 日志失败不影响主流程。
        }
    }

    /**
     * 填充日志内容。
     *
     * @param log 日志实体
     * @param result 检索结果
     * @param trace 检索快照
     * @param status 状态
     * @param errorMessage 错误信息
     */
    private void fillLog(KbRetrievalTestLog log,
                        KbSemanticSearchQueryDTO requestDto,
                        KbSemanticSearchResultVO result,
                        Map<String, Object> trace,
                        String status,
                        String errorMessage) {
        if (log == null) {
            return;
        }
        Map<String, Object> safeTrace = trace == null ? new LinkedHashMap<>() : trace;
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setOptimizedQueriesJson(JSON.toJSONString(safeTrace.get("optimized_queries")));
        log.setUsedQueriesJson(JSON.toJSONString(safeTrace.get("used_queries")));
        log.setParamsJson(buildParamsJson(requestDto, result, safeTrace));
        log.setDebugJson(JSON.toJSONString(safeTrace.get("debug_info")));
        if (result != null) {
            log.setSearchMode(result.getSearchMode());
            log.setResultCount(result.getResultCount());
            log.setResultJson(JSON.toJSONString(result));
            if (oConvertUtils.isNotEmpty(result.getQuery())) {
                log.setQuery(result.getQuery());
            }
        } else {
            Object actualParams = safeTrace.get("actual_params");
            if (actualParams instanceof Map) {
                Object searchMode = ((Map<?, ?>) actualParams).get("search_mode");
                if (searchMode != null) {
                    log.setSearchMode(String.valueOf(searchMode));
                }
            }
            Object resultCount = safeTrace.get("result_count");
            if (resultCount instanceof Number) {
                log.setResultCount(((Number) resultCount).intValue());
            }
            log.setResultJson("[]");
        }
        log.setUpdatedAt(new Date());
    }

    /**
     * 生成参数JSON。
     *
     * @param result 检索结果
     * @param trace 检索快照
     * @return JSON
     */
    private String buildParamsJson(KbSemanticSearchQueryDTO requestDto, KbSemanticSearchResultVO result, Map<String, Object> trace) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("request", requestDto == null ? new LinkedHashMap<>() : requestDto);
        params.put("actual_params", result == null ? (trace == null ? new LinkedHashMap<>() : trace.get("actual_params")) : result.getActualParams());
        return JSON.toJSONString(params);
    }
}
