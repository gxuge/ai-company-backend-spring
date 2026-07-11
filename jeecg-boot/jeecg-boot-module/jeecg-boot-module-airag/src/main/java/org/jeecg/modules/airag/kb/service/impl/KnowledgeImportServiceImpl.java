package org.jeecg.modules.airag.kb.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.io.FilenameUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.ImportConfirmDTO;
import org.jeecg.modules.airag.kb.dto.ImportTextDTO;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.jeecg.modules.airag.kb.entity.KbDocument;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkIndexMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkMapper;
import org.jeecg.modules.airag.kb.mapper.KbDocumentMapper;
import org.jeecg.modules.airag.kb.service.ChunkSplitter;
import org.jeecg.modules.airag.kb.service.DocumentParseService;
import org.jeecg.modules.airag.kb.service.KnowledgeImportService;
import org.jeecg.modules.airag.kb.vo.ChunkPreviewVO;
import org.jeecg.modules.airag.kb.vo.KbChunkIndexVo;
import org.jeecg.modules.airag.kb.vo.KbChunkVo;
import org.jeecg.modules.airag.kb.vo.KbDocumentVo;
import org.jeecg.modules.airag.kb.vo.KnowledgeImportResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 知识库导入服务实现。
 */
@Service
public class KnowledgeImportServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument> implements KnowledgeImportService {
    /**
     * 导入文件大小上限，默认10MB。
     */
    private static final long MAX_IMPORT_FILE_SIZE = 10L * 1024 * 1024;

    /**
     * 知识库主表Mapper。
     */
    private final KbBaseMapper kbBaseMapper;

    /**
     * chunk Mapper。
     */
    private final KbChunkMapper kbChunkMapper;

    /**
     * chunk索引Mapper。
     */
    private final KbChunkIndexMapper kbChunkIndexMapper;

    /**
     * 解析服务列表。
     */
    private final List<DocumentParseService> documentParseServices;

    /**
     * 切分器。
     */
    private final ChunkSplitter chunkSplitter;

    /**
     * 需要独立提交的事务模板。
     */
    private final TransactionTemplate requiresNewTemplate;

    /**
     * 常规事务模板。
     */
    private final TransactionTemplate requiredTemplate;

    /**
     * 构造方法。
     *
     * @param kbBaseMapper 知识库主表Mapper
     * @param kbChunkMapper chunk Mapper
     * @param kbChunkIndexMapper chunk索引Mapper
     * @param documentParseServices 解析服务列表
     * @param chunkSplitter 切分器
     * @param transactionManager 事务管理器
     */
    public KnowledgeImportServiceImpl(KbBaseMapper kbBaseMapper,
                                      KbChunkMapper kbChunkMapper,
                                      KbChunkIndexMapper kbChunkIndexMapper,
                                      List<DocumentParseService> documentParseServices,
                                      ChunkSplitter chunkSplitter,
                                      PlatformTransactionManager transactionManager) {
        this.kbBaseMapper = kbBaseMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbChunkIndexMapper = kbChunkIndexMapper;
        this.documentParseServices = documentParseServices;
        this.chunkSplitter = chunkSplitter;
        this.requiresNewTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.requiredTemplate = new TransactionTemplate(transactionManager);
        this.requiredTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    /**
     * 手动文本导入。
     *
     * @param kbId 知识库ID
     * @param dto 导入请求
     * @return 导入结果
     */
    @Override
    public KnowledgeImportResultVO importText(String kbId, ImportTextDTO dto) {
        KbBase kb = ensureKbEnabled(kbId);
        validateChunkConfig(dto.getChunkSize(), dto.getChunkOverlap());
        try {
            List<ChunkPreviewVO> previewList = chunkSplitter.split(dto.getContent(), dto.getChunkSize(), dto.getChunkOverlap());
            validatePreviewList(previewList);
            return requiredTemplate.execute(status -> {
                KbDocument draft = createDraftDocument(kbId, dto.getDocumentName(), KbConstants.SOURCE_TYPE_MANUAL, "text",
                        buildImportMetadata(KbConstants.SOURCE_TYPE_MANUAL, dto.getDocumentName(), dto.getChunkSize(), dto.getChunkOverlap(), "processing", "text", null, null, null));
                KnowledgeImportResultVO result = persistChunksAndIndexes(draft, previewList, KbConstants.SOURCE_TYPE_MANUAL, "text", dto.getChunkSize(), dto.getChunkOverlap(), "manual", kb.getId());
                markDocumentSuccess(draft.getId(), result.getChunkCount(), "text", dto.getChunkSize(), dto.getChunkOverlap(), KbConstants.SOURCE_TYPE_MANUAL, dto.getDocumentName());
                return result;
            });
        } catch (Exception e) {
            if (e instanceof JeecgBootException) {
                throw e;
            }
            throw new JeecgBootException(e.getMessage());
        }
    }

    /**
     * 文件导入。
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @param chunkSize 分段长度
     * @param chunkOverlap 分段重叠长度
     * @return 导入结果
     */
    @Override
    public KnowledgeImportResultVO importFile(String kbId, MultipartFile file, Integer chunkSize, Integer chunkOverlap) {
        KbBase kb = ensureKbEnabled(kbId);
        validateFile(file);
        validateChunkConfig(chunkSize, chunkOverlap);
        String fileName = resolveOriginalFilename(file);
        String extension = resolveExtension(fileName);
        ensureAllowedFileType(extension);
        try {
            String content = parseFile(file, extension);
            List<ChunkPreviewVO> previewList = chunkSplitter.split(content, chunkSize, chunkOverlap);
            validatePreviewList(previewList);
            return requiredTemplate.execute(status -> {
                KbDocument draft = createDraftDocument(kbId, stripExtension(fileName), KbConstants.SOURCE_TYPE_FILE, extension,
                        buildImportMetadata(KbConstants.SOURCE_TYPE_FILE, fileName, chunkSize, chunkOverlap, "processing", extension, null, null, null));
                KnowledgeImportResultVO result = persistChunksAndIndexes(draft, previewList, KbConstants.SOURCE_TYPE_FILE, extension, chunkSize, chunkOverlap, fileName, kb.getId());
                markDocumentSuccess(draft.getId(), result.getChunkCount(), extension, chunkSize, chunkOverlap, KbConstants.SOURCE_TYPE_FILE, fileName);
                return result;
            });
        } catch (Exception e) {
            if (e instanceof JeecgBootException) {
                throw e;
            }
            throw new JeecgBootException(e.getMessage());
        }
    }

    /**
     * 文本切分预览。
     *
     * @param kbId 知识库ID
     * @param dto 导入请求
     * @return chunk预览列表
     */
    @Override
    public List<ChunkPreviewVO> previewText(String kbId, ImportTextDTO dto) {
        ensureKbEnabled(kbId);
        validateChunkConfig(dto.getChunkSize(), dto.getChunkOverlap());
        List<ChunkPreviewVO> previewList = chunkSplitter.split(dto.getContent(), dto.getChunkSize(), dto.getChunkOverlap());
        validatePreviewList(previewList);
        return previewList;
    }

    /**
     * 文件切分预览。
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @param chunkSize 分段长度
     * @param chunkOverlap 分段重叠长度
     * @return chunk预览列表
     */
    @Override
    public List<ChunkPreviewVO> previewFile(String kbId, MultipartFile file, Integer chunkSize, Integer chunkOverlap) {
        ensureKbEnabled(kbId);
        validateFile(file);
        validateChunkConfig(chunkSize, chunkOverlap);
        String fileName = resolveOriginalFilename(file);
        String extension = resolveExtension(fileName);
        ensureAllowedFileType(extension);
        String content = parseFile(file, extension);
        List<ChunkPreviewVO> previewList = chunkSplitter.split(content, chunkSize, chunkOverlap);
        validatePreviewList(previewList);
        return previewList;
    }

    /**
     * 基于预览结果确认导入。
     *
     * @param kbId 知识库ID
     * @param dto 确认请求
     * @return 导入结果
     */
    @Override
    public KnowledgeImportResultVO confirmImport(String kbId, ImportConfirmDTO dto) {
        KbBase kb = ensureKbEnabled(kbId);
        List<ChunkPreviewVO> previewList = chunkSplitter.toPreviewList(dto.getChunks());
        validatePreviewList(previewList);
        try {
            return requiredTemplate.execute(status -> {
                KbDocument draft = createDraftDocument(kbId, dto.getDocumentName(), dto.getSourceType(), dto.getFileType(),
                        buildImportMetadata(dto.getSourceType(), dto.getDocumentName(), null, null, "processing", dto.getFileType(), null, null, null));
                KnowledgeImportResultVO result = persistChunksAndIndexes(draft, previewList, dto.getSourceType(), dto.getFileType(), null, null, null, kb.getId());
                markDocumentSuccess(draft.getId(), result.getChunkCount(), dto.getFileType(), null, null, dto.getSourceType(), dto.getDocumentName());
                return result;
            });
        } catch (Exception e) {
            if (e instanceof JeecgBootException) {
                throw e;
            }
            throw new JeecgBootException(e.getMessage());
        }
    }

    /**
     * 校验知识库是否存在且启用。
     *
     * @param kbId 知识库ID
     * @return 知识库实体
     */
    private KbBase ensureKbEnabled(String kbId) {
        KbBase kb = kbBaseMapper.selectById(kbId);
        if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        return kb;
    }

    /**
     * 校验chunk配置参数。
     *
     * @param chunkSize chunk长度
     * @param chunkOverlap chunk重叠长度
     */
    private void validateChunkConfig(Integer chunkSize, Integer chunkOverlap) {
        int size = chunkSize == null ? 800 : chunkSize;
        int overlap = chunkOverlap == null ? 100 : chunkOverlap;
        if (size <= 0) {
            throw new JeecgBootException("chunk_size必须大于0");
        }
        if (overlap < 0) {
            throw new JeecgBootException("chunk_overlap不能小于0");
        }
        if (overlap >= size) {
            throw new JeecgBootException("chunk_overlap必须小于chunk_size");
        }
    }

    /**
     * 校验文件。
     *
     * @param file 上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("文件不能为空");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new JeecgBootException("文件过大，不能超过10MB");
        }
    }

    /**
     * 校验预览结果不能为空。
     *
     * @param previewList 预览列表
     */
    private void validatePreviewList(List<ChunkPreviewVO> previewList) {
        if (previewList == null || previewList.isEmpty()) {
            throw new JeecgBootException("切分后chunk数不能为0");
        }
    }

    /**
     * 创建导入草稿文档。
     *
     * @param kbId 知识库ID
     * @param documentName 文档名称
     * @param sourceType 来源类型
     * @param fileType 文件类型
     * @param metadataJson 元数据JSON
     * @return 文档实体
     */
    private KbDocument createDraftDocument(String kbId, String documentName, String sourceType, String fileType, String metadataJson) {
        return requiresNewTemplate.execute(status -> {
            KbDocument document = new KbDocument();
            Date now = new Date();
            document.setKbId(kbId);
            document.setName(documentName);
            document.setSourceType(sourceType);
            document.setFileType(fileType);
            document.setParseStatus(KbConstants.PROCESS_STATUS_PROCESSING);
            document.setChunkStatus(KbConstants.PROCESS_STATUS_PROCESSING);
            document.setEmbedStatus(KbConstants.PROCESS_STATUS_PENDING);
            document.setMetadataJson(metadataJson);
            document.setStatus(KbConstants.STATUS_ENABLE);
            document.setCreatedAt(now);
            document.setUpdatedAt(now);
            kbBaseMapper.selectById(kbId);
            this.baseMapper.insert(document);
            return document;
        });
    }

    /**
     * 处理chunk和索引入库。
     *
     * @param draft 草稿文档
     * @param previewList 预览分段列表
     * @param sourceType 来源类型
     * @param fileType 文件类型
     * @param chunkSize 分段长度
     * @param chunkOverlap 分段重叠长度
     * @param sourceName 来源名称
     * @param kbId 知识库ID
     * @return 导入结果
     */
    private KnowledgeImportResultVO persistChunksAndIndexes(KbDocument draft,
                                                             List<ChunkPreviewVO> previewList,
                                                             String sourceType,
                                                             String fileType,
                                                             Integer chunkSize,
                                                             Integer chunkOverlap,
                                                             String sourceName,
                                                             String kbId) {
        Date now = new Date();
        List<KbChunkVo> chunkVoList = new ArrayList<>();
        int sortIndex = 1;
        for (ChunkPreviewVO preview : previewList) {
            if (preview == null || oConvertUtils.isEmpty(preview.getContent())) {
                continue;
            }
            KbChunk chunk = new KbChunk();
            chunk.setKbId(kbId);
            chunk.setDocumentId(draft.getId());
            chunk.setContent(preview.getContent().trim());
            chunk.setChunkType(KbConstants.CHUNK_TYPE_TEXT);
            chunk.setTokenCount(preview.getTokenCount() == null ? chunk.getContent().length() : preview.getTokenCount());
            chunk.setSortNo(preview.getSortNo() == null ? sortIndex : preview.getSortNo());
            chunk.setStatus(KbConstants.STATUS_ENABLE);
            chunk.setMetadataJson(buildChunkMetadata(sourceType, fileType, sourceName, chunkSize, chunkOverlap, preview.getSortNo()));
            chunk.setCreatedAt(now);
            chunk.setUpdatedAt(now);
            kbChunkMapper.insert(chunk);

            KbChunkIndex index = new KbChunkIndex();
            index.setKbId(kbId);
            index.setChunkId(chunk.getId());
            index.setIndexText(chunk.getContent());
            index.setIndexType(KbConstants.INDEX_TYPE_MANUAL);
            index.setEmbeddingStatus(KbConstants.PROCESS_STATUS_PENDING);
            index.setSortNo(chunk.getSortNo());
            index.setStatus(KbConstants.STATUS_ENABLE);
            index.setMetadataJson(buildIndexMetadata(sourceType, fileType, sourceName, preview.getSortNo()));
            index.setCreatedAt(now);
            index.setUpdatedAt(now);
            kbChunkIndexMapper.insert(index);

            KbChunkVo chunkVo = KbChunkVo.from(chunk);
            List<KbChunkIndexVo> indexVoList = new ArrayList<>();
            indexVoList.add(KbChunkIndexVo.from(index));
            chunkVo.setIndexList(indexVoList);
            chunkVoList.add(chunkVo);
            sortIndex++;
        }
        if (chunkVoList.isEmpty()) {
            throw new JeecgBootException("切分后chunk数不能为0");
        }
        KnowledgeImportResultVO result = KnowledgeImportResultVO.from(KbDocumentVo.from(draft), chunkVoList);
        return result;
    }

    /**
     * 更新文档为成功状态。
     *
     * @param documentId 文档ID
     * @param chunkCount chunk数量
     * @param fileType 文件类型
     * @param chunkSize chunk长度
     * @param chunkOverlap chunk重叠长度
     * @param sourceType 来源类型
     * @param sourceName 来源名称
     */
    private void markDocumentSuccess(String documentId,
                                     int chunkCount,
                                     String fileType,
                                     Integer chunkSize,
                                     Integer chunkOverlap,
                                     String sourceType,
                                     String sourceName) {
        KbDocument document = this.getById(documentId);
        if (document == null) {
            return;
        }
        document.setParseStatus(KbConstants.PROCESS_STATUS_SUCCESS);
        document.setChunkStatus(KbConstants.PROCESS_STATUS_SUCCESS);
        document.setEmbedStatus(KbConstants.PROCESS_STATUS_PENDING);
        document.setMetadataJson(buildImportMetadata(sourceType, sourceName, chunkSize, chunkOverlap, "success", fileType, chunkCount, null, null));
        document.setUpdatedAt(new Date());
        this.updateById(document);
    }

    /**
     * 更新文档为失败状态。
     *
     * @param documentId 文档ID
     * @param stage 失败阶段
     * @param errorMessage 错误信息
     * @param fileType 文件类型
     * @param chunkSize chunk长度
     * @param chunkOverlap chunk重叠长度
     * @param sourceType 来源类型
     */
    private void markDocumentFailed(String documentId,
                                    String stage,
                                    String errorMessage,
                                    String fileType,
                                    Integer chunkSize,
                                    Integer chunkOverlap,
                                    String sourceType) {
        requiresNewTemplate.execute(status -> {
            KbDocument document = this.getById(documentId);
            if (document == null) {
                return null;
            }
            document.setParseStatus(KbConstants.PROCESS_STATUS_FAILED);
            document.setChunkStatus(KbConstants.PROCESS_STATUS_FAILED);
            document.setEmbedStatus(KbConstants.PROCESS_STATUS_PENDING);
            document.setMetadataJson(buildImportMetadata(sourceType, null, chunkSize, chunkOverlap, "failed", fileType, null, stage, errorMessage));
            document.setUpdatedAt(new Date());
            this.updateById(document);
            return null;
        });
    }

    /**
     * 构建导入元数据。
     *
     * @param sourceType 来源类型
     * @param sourceName 来源名称
     * @param chunkSize chunk长度
     * @param chunkOverlap chunk重叠长度
     * @param importStatus 导入状态
     * @return JSON字符串
     */
    private String buildImportMetadata(String sourceType, String sourceName, Integer chunkSize, Integer chunkOverlap, String importStatus) {
        return buildImportMetadata(sourceType, sourceName, chunkSize, chunkOverlap, importStatus, null, null, null, null);
    }

    /**
     * 构建导入元数据。
     *
     * @param sourceType 来源类型
     * @param sourceName 来源名称
     * @param chunkSize chunk长度
     * @param chunkOverlap chunk重叠长度
     * @param importStatus 导入状态
     * @param fileType 文件类型
     * @param chunkCount chunk数量
     * @param errorStage 错误阶段
     * @param errorMessage 错误信息
     * @return JSON字符串
     */
    private String buildImportMetadata(String sourceType,
                                       String sourceName,
                                       Integer chunkSize,
                                       Integer chunkOverlap,
                                       String importStatus,
                                       String fileType,
                                       Integer chunkCount,
                                       String errorStage,
                                       String errorMessage) {
        JSONObject json = new JSONObject();
        if (oConvertUtils.isNotEmpty(sourceType)) {
            json.put("sourceType", sourceType);
        }
        if (oConvertUtils.isNotEmpty(sourceName)) {
            json.put("sourceName", sourceName);
        }
        if (chunkSize != null) {
            json.put("chunkSize", chunkSize);
        }
        if (chunkOverlap != null) {
            json.put("chunkOverlap", chunkOverlap);
        }
        if (oConvertUtils.isNotEmpty(importStatus)) {
            json.put("importStatus", importStatus);
        }
        if (oConvertUtils.isNotEmpty(fileType)) {
            json.put("fileType", fileType);
        }
        if (chunkCount != null) {
            json.put("chunkCount", chunkCount);
        }
        if (oConvertUtils.isNotEmpty(errorStage)) {
            json.put("errorStage", errorStage);
        }
        if (oConvertUtils.isNotEmpty(errorMessage)) {
            json.put("errorMessage", errorMessage);
        }
        return json.isEmpty() ? null : json.toJSONString();
    }

    /**
     * 构建chunk元数据。
     *
     * @param sourceType 来源类型
     * @param fileType 文件类型
     * @param sourceName 来源名称
     * @param chunkSize chunk长度
     * @param chunkOverlap chunk重叠长度
     * @param sortNo 排序号
     * @return JSON字符串
     */
    private String buildChunkMetadata(String sourceType, String fileType, String sourceName, Integer chunkSize, Integer chunkOverlap, Integer sortNo) {
        JSONObject json = new JSONObject();
        if (oConvertUtils.isNotEmpty(sourceType)) {
            json.put("sourceType", sourceType);
        }
        if (oConvertUtils.isNotEmpty(fileType)) {
            json.put("fileType", fileType);
        }
        if (oConvertUtils.isNotEmpty(sourceName)) {
            json.put("sourceName", sourceName);
        }
        if (chunkSize != null) {
            json.put("chunkSize", chunkSize);
        }
        if (chunkOverlap != null) {
            json.put("chunkOverlap", chunkOverlap);
        }
        if (sortNo != null) {
            json.put("sortNo", sortNo);
        }
        return json.isEmpty() ? null : json.toJSONString();
    }

    /**
     * 构建索引元数据。
     *
     * @param sourceType 来源类型
     * @param fileType 文件类型
     * @param sourceName 来源名称
     * @param sortNo 排序号
     * @return JSON字符串
     */
    private String buildIndexMetadata(String sourceType, String fileType, String sourceName, Integer sortNo) {
        JSONObject json = new JSONObject();
        if (oConvertUtils.isNotEmpty(sourceType)) {
            json.put("sourceType", sourceType);
        }
        if (oConvertUtils.isNotEmpty(fileType)) {
            json.put("fileType", fileType);
        }
        if (oConvertUtils.isNotEmpty(sourceName)) {
            json.put("sourceName", sourceName);
        }
        if (sortNo != null) {
            json.put("sortNo", sortNo);
        }
        return json.isEmpty() ? null : json.toJSONString();
    }

    /**
     * 解析文件内容。
     *
     * @param file 文件
     * @param extension 文件后缀
     * @return 文本内容
     */
    private String parseFile(MultipartFile file, String extension) {
        DocumentParseService parser = resolveParser(extension);
        String text = parser.parse(file);
        if (oConvertUtils.isEmpty(text)) {
            throw new JeecgBootException("文件内容不能为空");
        }
        return text;
    }

    /**
     * 选择解析服务。
     *
     * @param extension 文件后缀
     * @return 解析服务
     */
    private DocumentParseService resolveParser(String extension) {
        for (DocumentParseService parser : documentParseServices) {
            if (parser.supports(extension)) {
                return parser;
            }
        }
        throw new JeecgBootException("不支持的文件类型");
    }

    /**
     * 检查文件类型是否允许。
     *
     * @param extension 文件后缀
     */
    private void ensureAllowedFileType(String extension) {
        if (oConvertUtils.isEmpty(extension)) {
            throw new JeecgBootException("文件类型不能为空");
        }
        String ext = extension.toLowerCase(Locale.ROOT);
        if (!("txt".equals(ext) || "md".equals(ext) || "docx".equals(ext) || "pdf".equals(ext))) {
            throw new JeecgBootException("文件类型必须是txt、md、docx或pdf");
        }
    }

    /**
     * 获取原始文件名。
     *
     * @param file 文件
     * @return 文件名
     */
    private String resolveOriginalFilename(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (oConvertUtils.isEmpty(fileName)) {
            throw new JeecgBootException("文件名称不能为空");
        }
        return fileName;
    }

    /**
     * 提取文件后缀。
     *
     * @param fileName 文件名
     * @return 文件后缀
     */
    private String resolveExtension(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        if (oConvertUtils.isEmpty(extension)) {
            throw new JeecgBootException("文件类型不能为空");
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    /**
     * 去除文件后缀，作为文档名称。
     *
     * @param fileName 文件名
     * @return 文档名称
     */
    private String stripExtension(String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName);
        return oConvertUtils.isEmpty(baseName) ? fileName : baseName;
    }
}
