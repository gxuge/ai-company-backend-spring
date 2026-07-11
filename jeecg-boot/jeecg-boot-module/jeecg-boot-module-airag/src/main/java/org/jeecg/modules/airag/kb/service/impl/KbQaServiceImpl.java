package org.jeecg.modules.airag.kb.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbAutoQuestionDTO;
import org.jeecg.modules.airag.kb.dto.KbChunkCreateDto;
import org.jeecg.modules.airag.kb.dto.KbChunkIndexSaveDto;
import org.jeecg.modules.airag.kb.dto.KbDocumentCreateDto;
import org.jeecg.modules.airag.kb.dto.KbQaBatchDTO;
import org.jeecg.modules.airag.kb.dto.KbQaImportConfirmDTO;
import org.jeecg.modules.airag.kb.dto.KbQaItemDTO;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.jeecg.modules.airag.kb.entity.KbDocument;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkIndexMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkMapper;
import org.jeecg.modules.airag.kb.mapper.KbDocumentMapper;
import org.jeecg.modules.airag.kb.service.IKbChunkIndexService;
import org.jeecg.modules.airag.kb.service.IKbChunkService;
import org.jeecg.modules.airag.kb.service.IKbDocumentService;
import org.jeecg.modules.airag.kb.service.IKbQaService;
import org.jeecg.modules.airag.kb.service.KbEmbeddingService;
import org.jeecg.modules.airag.kb.vo.KbAutoQuestionItemVO;
import org.jeecg.modules.airag.kb.vo.KbAutoQuestionPreviewVO;
import org.jeecg.modules.airag.kb.vo.KbChunkIndexVo;
import org.jeecg.modules.airag.kb.vo.KbDocumentVo;
import org.jeecg.modules.airag.kb.vo.KbQaImportPreviewVO;
import org.jeecg.modules.airag.kb.vo.KbQaImportResultVO;
import org.jeecg.modules.airag.kb.vo.KbQaImportRowVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * QA导入与多索引服务实现。
 */
@Service
public class KbQaServiceImpl implements IKbQaService {
    /**
     * 知识库主表Mapper。
     */
    private final KbBaseMapper kbBaseMapper;

    /**
     * 文档服务。
     */
    private final IKbDocumentService kbDocumentService;

    /**
     * chunk服务。
     */
    private final IKbChunkService kbChunkService;

    /**
     * chunk索引服务。
     */
    private final IKbChunkIndexService kbChunkIndexService;

    /**
     * 文档Mapper。
     */
    private final KbDocumentMapper kbDocumentMapper;

    /**
     * chunk Mapper。
     */
    private final KbChunkMapper kbChunkMapper;

    /**
     * chunk索引Mapper。
     */
    private final KbChunkIndexMapper kbChunkIndexMapper;

    /**
     * embedding编排服务。
     */
    private final KbEmbeddingService kbEmbeddingService;

    /**
     * 构造方法。
     *
     * @param kbBaseMapper 知识库主表Mapper
     * @param kbDocumentService 文档服务
     * @param kbChunkService chunk服务
     * @param kbChunkIndexService chunk索引服务
     * @param kbDocumentMapper 文档Mapper
     * @param kbChunkMapper chunk Mapper
     * @param kbChunkIndexMapper chunk索引Mapper
     * @param kbEmbeddingService embedding编排服务
     */
    public KbQaServiceImpl(KbBaseMapper kbBaseMapper,
                           IKbDocumentService kbDocumentService,
                           IKbChunkService kbChunkService,
                           IKbChunkIndexService kbChunkIndexService,
                           KbDocumentMapper kbDocumentMapper,
                           KbChunkMapper kbChunkMapper,
                           KbChunkIndexMapper kbChunkIndexMapper,
                           KbEmbeddingService kbEmbeddingService) {
        this.kbBaseMapper = kbBaseMapper;
        this.kbDocumentService = kbDocumentService;
        this.kbChunkService = kbChunkService;
        this.kbChunkIndexService = kbChunkIndexService;
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbChunkIndexMapper = kbChunkIndexMapper;
        this.kbEmbeddingService = kbEmbeddingService;
    }

    /**
     * 手动新增单条QA。
     *
     * @param kbId 知识库ID
     * @param dto QA条目
     * @return 导入结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbQaImportResultVO createQa(String kbId, KbQaItemDTO dto) {
        List<KbQaItemDTO> items = new ArrayList<>();
        items.add(dto);
        return importQaItems(kbId, items, buildSingleDocumentName(dto), KbConstants.SOURCE_TYPE_QA, KbConstants.SOURCE_TYPE_QA);
    }

    /**
     * 批量新增QA。
     *
     * @param kbId 知识库ID
     * @param dto 批量请求
     * @return 导入结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbQaImportResultVO batchCreateQa(String kbId, KbQaBatchDTO dto) {
        return importQaItems(kbId, dto == null ? null : dto.getItems(), buildBatchDocumentName(), KbConstants.SOURCE_TYPE_QA, KbConstants.SOURCE_TYPE_QA);
    }

    /**
     * CSV/Excel导入预览。
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @return 预览结果
     */
    @Override
    public KbQaImportPreviewVO previewImport(String kbId, MultipartFile file) {
        ensureKbEnabled(kbId);
        List<KbQaItemDTO> items = KbQaImportFileParser.parse(file);
        if (items.isEmpty()) {
            throw new JeecgBootException("未解析到有效QA数据");
        }
        return buildPreview(kbId, items);
    }

    /**
     * CSV/Excel确认导入。
     *
     * @param kbId 知识库ID
     * @param dto 确认请求
     * @return 导入结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbQaImportResultVO confirmImport(String kbId, KbQaImportConfirmDTO dto) {
        if (dto == null) {
            throw new JeecgBootException("确认导入参数不能为空");
        }
        String sourceType = normalizeSourceType(dto.getSourceType());
        String fileType = normalizeFileType(dto.getFileType());
        return importQaItems(kbId, dto.getItems(), dto.getDocumentName(), sourceType, fileType);
    }

    /**
     * 预览自动生成的索引问题（基于chunk）。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @param dto 请求
     * @return 预览结果
     */
    @Override
    public KbAutoQuestionPreviewVO previewAutoQuestions(String kbId, String chunkId, KbAutoQuestionDTO dto) {
        KbChunk chunk = ensureChunkEnabled(kbId, chunkId);
        return buildAutoQuestionPreview(chunk.getId(), chunk.getContent(), dto);
    }

    /**
     * 预览自动生成的索引问题（基于文本）。
     *
     * @param kbId 知识库ID
     * @param dto 请求
     * @return 预览结果
     */
    @Override
    public KbAutoQuestionPreviewVO previewAutoQuestions(String kbId, KbAutoQuestionDTO dto) {
        ensureKbEnabled(kbId);
        if (dto == null || oConvertUtils.isEmpty(dto.getContent())) {
            throw new JeecgBootException("content不能为空");
        }
        return buildAutoQuestionPreview(null, dto.getContent(), dto);
    }

    /**
     * 保存自动生成的索引问题。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @param dto 请求
     * @return 创建的索引列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KbChunkIndexVo> saveAutoQuestions(String kbId, String chunkId, KbAutoQuestionDTO dto) {
        KbChunk chunk = ensureChunkEnabled(kbId, chunkId);
        int questionCount = resolveQuestionCount(dto);
        String content = oConvertUtils.isNotEmpty(dto == null ? null : dto.getContent()) ? dto.getContent() : chunk.getContent();
        List<String> generated = generateQuestions(content, questionCount);
        if (generated.isEmpty()) {
            throw new JeecgBootException("自动生成索引问题数量为空");
        }
        if (dto != null && Boolean.TRUE.equals(dto.getOverwriteAutoQuestion())) {
            deleteAutoQuestionIndexes(chunk.getId());
        }
        Set<String> existing = loadExistingIndexTextSet(chunk.getKbId(), chunk.getId());
        List<KbChunkIndexSaveDto> saveDtos = new ArrayList<>();
        AtomicInteger sortNo = new AtomicInteger(1);
        for (String question : generated) {
            String normalized = normalizeIndexText(question);
            if (oConvertUtils.isEmpty(normalized) || existing.contains(normalized)) {
                continue;
            }
            KbChunkIndexSaveDto saveDto = new KbChunkIndexSaveDto();
            saveDto.setIndexText(normalized);
            saveDto.setIndexType(KbConstants.INDEX_TYPE_AUTO_QUESTION);
            saveDto.setEmbeddingStatus(KbConstants.PROCESS_STATUS_PENDING);
            saveDto.setStatus(KbConstants.STATUS_ENABLE);
            saveDto.setSortNo(sortNo.getAndIncrement());
            saveDto.setMetadataJson(buildAutoQuestionMetadata(chunk.getId(), normalized, dto));
            saveDtos.add(saveDto);
            existing.add(normalized);
        }
        if (saveDtos.isEmpty()) {
            return new ArrayList<>();
        }
        return kbChunkIndexService.createIndexes(kbId, chunkId, saveDtos);
    }

    /**
     * 导入QA条目。
     *
     * @param kbId 知识库ID
     * @param items 条目列表
     * @param documentName 文档名称
     * @param sourceType 来源类型
     * @param fileType 文件类型
     * @return 导入结果
     */
    private KbQaImportResultVO importQaItems(String kbId, List<KbQaItemDTO> items, String documentName, String sourceType, String fileType) {
        ensureKbEnabled(kbId);
        List<KbQaItemDTO> safeItems = items == null ? new ArrayList<>() : items;
        KbQaImportResultVO result = new KbQaImportResultVO();
        result.setTotal(safeItems.size());
        if (safeItems.isEmpty()) {
            result.setSuccessCount(0);
            result.setFailedCount(0);
            result.setSkippedCount(0);
            return result;
        }
        List<RowValidation> validations = validateRows(kbId, safeItems, true);
        List<KbQaItemDTO> validItems = validations.stream()
                .filter(RowValidation::isValid)
                .map(RowValidation::getItem)
                .collect(Collectors.toList());
        int failed = 0;
        int skipped = 0;
        for (RowValidation validation : validations) {
            if (!validation.isValid()) {
                if (validation.isDuplicate()) {
                    skipped++;
                } else {
                    failed++;
                }
                result.getErrors().add(toErrorRow(validation));
            }
        }
        if (validItems.isEmpty()) {
            result.setSuccessCount(0);
            result.setFailedCount(failed);
            result.setSkippedCount(skipped);
            return result;
        }

        String finalDocumentName = oConvertUtils.isNotEmpty(documentName) ? documentName : buildBatchDocumentName();
        KbDocument document = createQaDocument(kbId, finalDocumentName, sourceType, fileType, validItems);
        int success = 0;
        try {
            int sequence = 1;
            for (KbQaItemDTO item : validItems) {
                createQaChunk(document, item, sequence++);
                success++;
            }
            result.setSuccessCount(success);
            result.setFailedCount(failed);
            result.setSkippedCount(skipped);
            return result;
        } catch (Exception e) {
            throw new JeecgBootException("QA写库失败：" + e.getMessage());
        }
    }

    /**
     * 创建QA文档。
     *
     * @param kbId 知识库ID
     * @param documentName 文档名称
     * @param sourceType 来源类型
     * @param fileType 文件类型
     * @param items QA条目
     * @return 文档实体
     */
    private KbDocument createQaDocument(String kbId, String documentName, String sourceType, String fileType, List<KbQaItemDTO> items) {
        KbDocumentCreateDto createDto = new KbDocumentCreateDto();
        createDto.setName(documentName);
        createDto.setSourceType(normalizeSourceType(sourceType));
        createDto.setFileType(normalizeFileType(fileType));
        createDto.setParseStatus(KbConstants.PROCESS_STATUS_SUCCESS);
        createDto.setChunkStatus(KbConstants.PROCESS_STATUS_SUCCESS);
        createDto.setEmbedStatus(KbConstants.PROCESS_STATUS_PENDING);
        createDto.setStatus(KbConstants.STATUS_ENABLE);
        JSONObject metadata = new JSONObject();
        metadata.put("import_type", "qa");
        metadata.put("qa_count", items == null ? 0 : items.size());
        metadata.put("source_type", createDto.getSourceType());
        metadata.put("file_type", createDto.getFileType());
        metadata.put("created_at", new Date());
        createDto.setMetadataJson(metadata.toJSONString());
        KbDocumentVo vo = kbDocumentService.createDocument(kbId, createDto);
        KbDocument document = kbDocumentMapper.selectById(vo.getId());
        if (document == null) {
            throw new JeecgBootException("创建QA文档失败");
        }
        return document;
    }

    /**
     * 创建QA chunk及默认索引。
     *
     * @param document 文档
     * @param item QA条目
     * @param sequence 序号
     */
    private void createQaChunk(KbDocument document, KbQaItemDTO item, int sequence) {
        KbChunkCreateDto chunkDto = new KbChunkCreateDto();
        chunkDto.setDocumentId(document.getId());
        chunkDto.setContent(normalizeContent(item.getAnswer()));
        chunkDto.setChunkType(KbConstants.CHUNK_TYPE_QA);
        chunkDto.setTokenCount(estimateTokenCount(item.getAnswer()));
        chunkDto.setSortNo(item.getSortNo() == null ? sequence : item.getSortNo());
        chunkDto.setStatus(KbConstants.STATUS_ENABLE);
        JSONObject chunkMetadata = new JSONObject();
        chunkMetadata.put("qa_question", item.getQuestion());
        chunkMetadata.put("qa_tags", item.getTags());
        chunkMetadata.put("qa_row_no", item.getRowNo());
        chunkMetadata.put("qa_sort_no", item.getSortNo());
        chunkMetadata.put("qa_metadata_json", item.getMetadataJson());
        chunkDto.setMetadataJson(chunkMetadata.toJSONString());

        KbChunkIndexSaveDto indexDto = new KbChunkIndexSaveDto();
        indexDto.setIndexText(normalizeIndexText(item.getQuestion()));
        indexDto.setIndexType(KbConstants.INDEX_TYPE_QUESTION);
        indexDto.setEmbeddingStatus(KbConstants.PROCESS_STATUS_PENDING);
        indexDto.setStatus(KbConstants.STATUS_ENABLE);
        indexDto.setSortNo(chunkDto.getSortNo());
        indexDto.setMetadataJson(buildQuestionIndexMetadata(document.getId(), item));
        chunkDto.setIndexList(new ArrayList<>());
        chunkDto.getIndexList().add(indexDto);
        kbChunkService.createChunk(document.getKbId(), chunkDto);
    }

    /**
     * 构建QA预览。
     *
     * @param kbId 知识库ID
     * @param items 条目列表
     * @return 预览结果
     */
    private KbQaImportPreviewVO buildPreview(String kbId, List<KbQaItemDTO> items) {
        List<RowValidation> validations = validateRows(kbId, items, false);
        KbQaImportPreviewVO vo = new KbQaImportPreviewVO();
        vo.setTotal(validations.size());
        int success = 0;
        int failed = 0;
        for (RowValidation validation : validations) {
            KbQaImportRowVO rowVO = new KbQaImportRowVO();
            rowVO.setRowNo(validation.getItem() == null ? validation.getRowNo() : validation.getItem().getRowNo());
            rowVO.setQuestion(validation.getItem() == null ? null : validation.getItem().getQuestion());
            rowVO.setAnswer(validation.getItem() == null ? null : validation.getItem().getAnswer());
            rowVO.setTags(validation.getItem() == null ? null : validation.getItem().getTags());
            rowVO.setMetadataJson(validation.getItem() == null ? null : validation.getItem().getMetadataJson());
            rowVO.setSortNo(validation.getItem() == null ? null : validation.getItem().getSortNo());
            rowVO.setValid(validation.isValid());
            rowVO.setErrorMessage(validation.getErrorMessage());
            vo.getItems().add(rowVO);
            if (validation.isValid()) {
                success++;
            } else {
                failed++;
            }
        }
        vo.setSuccessCount(success);
        vo.setFailedCount(failed);
        return vo;
    }

    /**
     * 校验QA行。
     *
     * @param kbId 知识库ID
     * @param items 条目
     * @param checkDuplicateWithBatch 是否检查批内重复并用于持久化流程
     * @return 校验结果
     */
    private List<RowValidation> validateRows(String kbId, List<KbQaItemDTO> items, boolean checkDuplicateWithBatch) {
        List<RowValidation> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        Set<String> existingIndexTexts = loadExistingIndexTextSet(kbId, null);
        Set<String> batchIndexTexts = new LinkedHashSet<>();
        for (KbQaItemDTO item : items) {
            RowValidation validation = new RowValidation(item == null ? null : item.getRowNo(), item);
            if (item == null) {
                validation.fail("QA条目不能为空", false);
                result.add(validation);
                continue;
            }
            String question = normalizeIndexText(item.getQuestion());
            String answer = normalizeContent(item.getAnswer());
            if (oConvertUtils.isEmpty(question)) {
                validation.fail("question不能为空", false);
            } else if (question.length() > 4000) {
                validation.fail("question不能超过4000个字符", false);
            } else if (oConvertUtils.isEmpty(answer)) {
                validation.fail("answer不能为空", false);
            } else if (!isValidJson(item.getMetadataJson())) {
                validation.fail("metadata_json不是合法JSON", false);
            } else if (isDuplicateQuestion(existingIndexTexts, batchIndexTexts, question)) {
                validation.fail("question重复", true);
            } else {
                validation.pass(item);
                batchIndexTexts.add(question);
                if (checkDuplicateWithBatch) {
                    existingIndexTexts.add(question);
                }
            }
            result.add(validation);
        }
        return result;
    }

    /**
     * 判断是否重复。
     *
     * @param existing 已存在集合
     * @param batch 批次集合
     * @param question 问题
     * @return 是否重复
     */
    private boolean isDuplicateQuestion(Set<String> existing, Set<String> batch, String question) {
        return (existing != null && existing.contains(question)) || (batch != null && batch.contains(question));
    }

    /**
     * 加载已存在的索引文本。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID，可为空
     * @return 索引文本集合
     */
    private Set<String> loadExistingIndexTextSet(String kbId, String chunkId) {
        LambdaQueryWrapper<KbChunkIndex> wrapper = new LambdaQueryWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getKbId, kbId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE);
        if (oConvertUtils.isNotEmpty(chunkId)) {
            wrapper.eq(KbChunkIndex::getChunkId, chunkId);
        }
        List<KbChunkIndex> indexes = kbChunkIndexMapper.selectList(wrapper);
        Set<String> result = new LinkedHashSet<>();
        for (KbChunkIndex index : indexes) {
            String text = normalizeIndexText(index.getIndexText());
            if (oConvertUtils.isNotEmpty(text)) {
                result.add(text);
            }
        }
        return result;
    }

    /**
     * 构建自动问题预览。
     *
     * @param chunkId chunk ID
     * @param content 内容
     * @param dto 请求
     * @return 预览结果
     */
    private KbAutoQuestionPreviewVO buildAutoQuestionPreview(String chunkId, String content, KbAutoQuestionDTO dto) {
        int questionCount = resolveQuestionCount(dto);
        if (oConvertUtils.isEmpty(content)) {
            throw new JeecgBootException("content不能为空");
        }
        List<String> questions = generateQuestions(content, questionCount);
        if (questions.isEmpty()) {
            throw new JeecgBootException("自动生成索引问题数量为空");
        }
        KbAutoQuestionPreviewVO vo = new KbAutoQuestionPreviewVO();
        vo.setChunkId(chunkId);
        vo.setContent(content);
        AtomicInteger sortNo = new AtomicInteger(1);
        for (String question : questions) {
            KbAutoQuestionItemVO item = new KbAutoQuestionItemVO();
            item.setIndexText(normalizeIndexText(question));
            item.setIndexType(KbConstants.INDEX_TYPE_AUTO_QUESTION);
            item.setSortNo(sortNo.getAndIncrement());
            vo.getItems().add(item);
        }
        return vo;
    }

    /**
     * 生成自动问题。
     *
     * @param content 内容
     * @param questionCount 数量
     * @return 问题列表
     */
    private List<String> generateQuestions(String content, int questionCount) {
        if (oConvertUtils.isEmpty(content)) {
            return new ArrayList<>();
        }
        String normalized = content.trim().replace("\r", "\n");
        String[] primaryParts = normalized.split("(?<=[。！？!?\\n])");
        List<String> candidates = new ArrayList<>();
        for (String part : primaryParts) {
            String text = part == null ? null : part.trim();
            if (oConvertUtils.isNotEmpty(text)) {
                candidates.add(text);
            }
            if (candidates.size() >= questionCount) {
                break;
            }
        }
        if (candidates.isEmpty()) {
            candidates.add(normalized);
        }
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            result.add(buildQuestionFromContent(candidate));
            if (result.size() >= questionCount) {
                break;
            }
        }
        while (result.size() < questionCount) {
            result.add(buildFallbackQuestion(result.size() + 1));
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 根据内容生成问题。
     *
     * @param content 内容片段
     * @return 问题
     */
    private String buildQuestionFromContent(String content) {
        String text = normalizeIndexText(content);
        if (oConvertUtils.isEmpty(text)) {
            return buildFallbackQuestion(1);
        }
        if (text.length() > 24) {
            text = text.substring(0, 24);
        }
        return "关于" + text + "，应该如何理解？";
    }

    /**
     * 构建兜底问题。
     *
     * @param index 序号
     * @return 问题
     */
    private String buildFallbackQuestion(int index) {
        switch (index) {
            case 1:
                return "这段内容的核心是什么？";
            case 2:
                return "这段内容适合什么场景？";
            case 3:
                return "这段内容有哪些关键点？";
            case 4:
                return "这段内容需要注意什么？";
            default:
                return "这段内容如何应用？";
        }
    }

    /**
     * 解析QA问题数量。
     *
     * @param dto 请求
     * @return 数量
     */
    private int resolveQuestionCount(KbAutoQuestionDTO dto) {
        int count = dto == null || dto.getQuestionCount() == null ? KbConstants.DEFAULT_AUTO_QUESTION_COUNT : dto.getQuestionCount();
        if (count <= 0) {
            throw new JeecgBootException("自动生成索引问题数量必须大于0");
        }
        return Math.min(count, KbConstants.MAX_AUTO_QUESTION_COUNT);
    }

    /**
     * 删除chunk下的自动问题索引。
     *
     * @param chunkId chunk ID
     */
    private void deleteAutoQuestionIndexes(String chunkId) {
        List<KbChunkIndex> autoIndexes = kbChunkIndexMapper.selectList(new LambdaQueryWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getChunkId, chunkId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                .eq(KbChunkIndex::getIndexType, KbConstants.INDEX_TYPE_AUTO_QUESTION));
        if (autoIndexes.isEmpty()) {
            return;
        }
        for (KbChunkIndex index : autoIndexes) {
            kbChunkIndexService.deleteIndex(index.getId());
        }
    }

    /**
     * 构建QA索引元数据。
     *
     * @param documentId 文档ID
     * @param item QA条目
     * @return JSON字符串
     */
    private String buildQuestionIndexMetadata(String documentId, KbQaItemDTO item) {
        JSONObject json = new JSONObject();
        json.put("document_id", documentId);
        json.put("question", item.getQuestion());
        json.put("tags", item.getTags());
        json.put("row_no", item.getRowNo());
        json.put("sort_no", item.getSortNo());
        json.put("metadata_json", item.getMetadataJson());
        json.put("index_type", KbConstants.INDEX_TYPE_QUESTION);
        return json.toJSONString();
    }

    /**
     * 构建自动问题元数据。
     *
     * @param chunkId chunk ID
     * @param question 问题
     * @param dto 请求
     * @return JSON字符串
     */
    private String buildAutoQuestionMetadata(String chunkId, String question, KbAutoQuestionDTO dto) {
        JSONObject json = new JSONObject();
        json.put("chunk_id", chunkId);
        json.put("question", question);
        json.put("index_type", KbConstants.INDEX_TYPE_AUTO_QUESTION);
        json.put("overwrite_auto_question", dto != null && Boolean.TRUE.equals(dto.getOverwriteAutoQuestion()));
        return json.toJSONString();
    }

    /**
     * 校验JSON是否合法。
     *
     * @param jsonText JSON文本
     * @return 是否合法
     */
    private boolean isValidJson(String jsonText) {
        if (oConvertUtils.isEmpty(jsonText)) {
            return true;
        }
        try {
            JSONObject.parse(jsonText);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 构建单条文档名。
     *
     * @param dto QA条目
     * @return 文档名
     */
    private String buildSingleDocumentName(KbQaItemDTO dto) {
        if (dto != null && oConvertUtils.isNotEmpty(dto.getQuestion())) {
            String text = dto.getQuestion().trim();
            if (text.length() > 16) {
                text = text.substring(0, 16);
            }
            return "QA-" + text;
        }
        return buildBatchDocumentName();
    }

    /**
     * 构建批量文档名。
     *
     * @return 文档名
     */
    private String buildBatchDocumentName() {
        return "QA导入-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 归一化来源类型。
     *
     * @param sourceType 来源类型
     * @return 来源类型
     */
    private String normalizeSourceType(String sourceType) {
        if (oConvertUtils.isEmpty(sourceType)) {
            return KbConstants.SOURCE_TYPE_QA;
        }
        return sourceType.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 归一化文件类型。
     *
     * @param fileType 文件类型
     * @return 文件类型
     */
    private String normalizeFileType(String fileType) {
        if (oConvertUtils.isEmpty(fileType)) {
            return "qa";
        }
        return fileType.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 归一化内容。
     *
     * @param content 内容
     * @return 内容
     */
    private String normalizeContent(String content) {
        if (oConvertUtils.isEmpty(content)) {
            return null;
        }
        String value = content.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * 归一化索引文本。
     *
     * @param text 文本
     * @return 文本
     */
    private String normalizeIndexText(String text) {
        if (oConvertUtils.isEmpty(text)) {
            return null;
        }
        String value = text.trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > 4000) {
            return value.substring(0, 4000);
        }
        return value;
    }

    /**
     * 估算token数量。
     *
     * @param text 文本
     * @return token数量
     */
    private int estimateTokenCount(String text) {
        if (oConvertUtils.isEmpty(text)) {
            return 0;
        }
        return text.trim().length();
    }

    /**
     * 确保知识库启用。
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
     * 确保chunk启用且属于当前知识库。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @return chunk实体
     */
    private KbChunk ensureChunkEnabled(String kbId, String chunkId) {
        KbChunk chunk = kbChunkMapper.selectById(chunkId);
        if (chunk == null || KbConstants.STATUS_DISABLE.equals(chunk.getStatus())) {
            throw new JeecgBootException("未找到对应chunk");
        }
        if (!kbId.equals(chunk.getKbId())) {
            throw new JeecgBootException("chunk不属于当前知识库");
        }
        return chunk;
    }

    /**
     * 行校验结果。
     */
    private static class RowValidation {
        /**
         * 行号。
         */
        private final Integer rowNo;

        /**
         * 条目。
         */
        private KbQaItemDTO item;

        /**
         * 是否有效。
         */
        private boolean valid;

        /**
         * 是否重复。
         */
        private boolean duplicate;

        /**
         * 错误信息。
         */
        private String errorMessage;

        /**
         * 构造方法。
         *
         * @param rowNo 行号
         * @param item 条目
         */
        private RowValidation(Integer rowNo, KbQaItemDTO item) {
            this.rowNo = rowNo;
            this.item = item;
        }

        /**
         * 通过校验。
         *
         * @param item 条目
         */
        private void pass(KbQaItemDTO item) {
            this.valid = true;
            this.item = item;
        }

        /**
         * 失败。
         *
         * @param message 错误信息
         * @param duplicate 是否重复
         */
        private void fail(String message, boolean duplicate) {
            this.valid = false;
            this.duplicate = duplicate;
            this.errorMessage = message;
        }

        private Integer getRowNo() {
            return rowNo;
        }

        private KbQaItemDTO getItem() {
            return item;
        }

        private boolean isValid() {
            return valid;
        }

        private boolean isDuplicate() {
            return duplicate;
        }

        private String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 转换错误行。
     *
     * @param validation 校验结果
     * @return 错误行
     */
    private KbQaImportRowVO toErrorRow(RowValidation validation) {
        KbQaImportRowVO vo = new KbQaImportRowVO();
        vo.setRowNo(validation.getRowNo());
        vo.setQuestion(validation.getItem() == null ? null : validation.getItem().getQuestion());
        vo.setAnswer(validation.getItem() == null ? null : validation.getItem().getAnswer());
        vo.setTags(validation.getItem() == null ? null : validation.getItem().getTags());
        vo.setMetadataJson(validation.getItem() == null ? null : validation.getItem().getMetadataJson());
        vo.setSortNo(validation.getItem() == null ? null : validation.getItem().getSortNo());
        vo.setValid(false);
        vo.setErrorMessage(validation.getErrorMessage());
        return vo;
    }
}
