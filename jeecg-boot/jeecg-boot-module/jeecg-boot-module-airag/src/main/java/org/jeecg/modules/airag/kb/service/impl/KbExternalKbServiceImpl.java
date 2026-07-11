package org.jeecg.modules.airag.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.dto.KbExternalKbQueryDto;
import org.jeecg.modules.airag.kb.dto.KbExternalKbSaveDto;
import org.jeecg.modules.airag.kb.entity.KbExternalKb;
import org.jeecg.modules.airag.kb.mapper.KbExternalKbMapper;
import org.jeecg.modules.airag.kb.service.IKbExternalKbService;
import org.jeecg.modules.airag.kb.vo.KbExternalKbVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 外部知识库服务实现。
 */
@Service
public class KbExternalKbServiceImpl extends ServiceImpl<KbExternalKbMapper, KbExternalKb> implements IKbExternalKbService {
    /**
     * HTTP客户端。
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbExternalKbVo create(KbExternalKbSaveDto dto) {
        validateEndpoint(dto == null ? null : dto.getEndpointUrl());
        KbExternalKb entity = dto.toEntity();
        this.save(entity);
        return KbExternalKbVo.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbExternalKbVo update(String id, KbExternalKbSaveDto dto) {
        KbExternalKb entity = this.getById(id);
        if (entity == null) {
            throw new JeecgBootException("未找到对应外部知识库");
        }
        validateEndpoint(dto == null ? null : dto.getEndpointUrl());
        entity.setExternalKbId(dto.getExternalKbId());
        entity.setName(dto.getName());
        entity.setEnabled(dto.getEnabled());
        entity.setEndpointUrl(dto.getEndpointUrl());
        entity.setAuthType(dto.getAuthType() == null ? "none" : dto.getAuthType());
        entity.setAuthConfig(dto.getAuthConfig());
        entity.setTimeoutMs(dto.getTimeoutMs() == null ? 5000 : dto.getTimeoutMs());
        entity.setWeight(dto.getWeight() == null ? java.math.BigDecimal.ONE : dto.getWeight());
        entity.setMetadataJson(dto.getMetadataJson());
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
        return KbExternalKbVo.from(entity);
    }

    @Override
    public void delete(String id) {
        KbExternalKb entity = this.getById(id);
        if (entity == null) {
            throw new JeecgBootException("未找到对应外部知识库");
        }
        entity.setEnabled(Boolean.FALSE);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
    }

    @Override
    public KbExternalKbVo getDetail(String id) {
        KbExternalKb entity = this.getById(id);
        if (entity == null) {
            throw new JeecgBootException("未找到对应外部知识库");
        }
        return KbExternalKbVo.from(entity);
    }

    @Override
    public IPage<KbExternalKbVo> page(KbExternalKbQueryDto query) {
        int pageNo = query == null || query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query == null || query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        Page<KbExternalKb> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<KbExternalKb> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (oConvertUtils.isNotEmpty(query.getExternalKbId())) {
                wrapper.eq(KbExternalKb::getExternalKbId, query.getExternalKbId());
            }
            if (oConvertUtils.isNotEmpty(query.getName())) {
                wrapper.like(KbExternalKb::getName, query.getName());
            }
            if (query.getEnabled() != null) {
                wrapper.eq(KbExternalKb::getEnabled, query.getEnabled());
            }
        }
        wrapper.orderByDesc(KbExternalKb::getUpdatedAt).orderByDesc(KbExternalKb::getCreatedAt);
        IPage<KbExternalKb> pageData = this.page(page, wrapper);
        Page<KbExternalKbVo> voPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        List<KbExternalKbVo> records = new ArrayList<>();
        for (KbExternalKb entity : pageData.getRecords()) {
            records.add(KbExternalKbVo.from(entity));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public String testConnection(String id) {
        KbExternalKb entity = this.getById(id);
        if (entity == null) {
            throw new JeecgBootException("未找到对应外部知识库");
        }
        if (Boolean.FALSE.equals(entity.getEnabled())) {
            throw new JeecgBootException("外部知识库已禁用");
        }
        validateEndpoint(entity.getEndpointUrl());
        try {
            URI uri = URI.create(entity.getEndpointUrl());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(java.time.Duration.ofMillis(entity.getTimeoutMs() == null ? 5000 : entity.getTimeoutMs()))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception headEx) {
                HttpRequest getRequest = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(java.time.Duration.ofMillis(entity.getTimeoutMs() == null ? 5000 : entity.getTimeoutMs()))
                        .GET()
                        .build();
                response = httpClient.send(getRequest, HttpResponse.BodyHandlers.discarding());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 400) {
                throw new JeecgBootException("外部知识库连接失败，HTTP状态=" + response.statusCode());
            }
            return "ok";
        } catch (IllegalArgumentException ex) {
            throw new JeecgBootException("endpoint_url非法");
        } catch (JeecgBootException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new JeecgBootException("外部知识库连接失败：" + ex.getMessage());
        }
    }

    @Override
    public KbExternalKbVo getByExternalKbId(String externalKbId) {
        return KbExternalKbVo.from(getEntityByExternalKbId(externalKbId));
    }

    @Override
    public KbExternalKb getEntityByExternalKbId(String externalKbId) {
        if (oConvertUtils.isEmpty(externalKbId)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<KbExternalKb>().eq(KbExternalKb::getExternalKbId, externalKbId), false);
    }

    /**
     * 校验地址。
     *
     * @param endpointUrl 地址
     */
    private void validateEndpoint(String endpointUrl) {
        if (oConvertUtils.isEmpty(endpointUrl)) {
            throw new JeecgBootException("endpoint_url不能为空");
        }
        try {
            URI uri = URI.create(endpointUrl);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                throw new JeecgBootException("endpoint_url非法");
            }
            String normalizedScheme = scheme.toLowerCase(java.util.Locale.ROOT);
            if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
                throw new JeecgBootException("endpoint_url非法");
            }
        } catch (IllegalArgumentException ex) {
            throw new JeecgBootException("endpoint_url非法");
        }
    }
}
