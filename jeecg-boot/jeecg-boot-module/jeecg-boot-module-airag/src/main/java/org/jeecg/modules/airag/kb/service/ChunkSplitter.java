package org.jeecg.modules.airag.kb.service;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.dto.ImportConfirmDTO;
import org.jeecg.modules.airag.kb.vo.ChunkPreviewVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 基础chunk切分器。
 */
@Component
public class ChunkSplitter {
    /**
     * 默认chunk长度。
     */
    private static final int DEFAULT_CHUNK_SIZE = 800;

    /**
     * 默认chunk重叠长度。
     */
    private static final int DEFAULT_CHUNK_OVERLAP = 100;

    /**
     * 分隔符优先级。
     */
    private static final List<String> SEPARATORS = Arrays.asList("\n\n", "\n", "。", ".", " ", "");

    /**
     * 将文本切分为chunk预览。
     *
     * @param content 文本内容
     * @param chunkSize chunk长度
     * @param chunkOverlap chunk重叠长度
     * @return chunk预览列表
     */
    public List<ChunkPreviewVO> split(String content, Integer chunkSize, Integer chunkOverlap) {
        int size = chunkSize == null || chunkSize < 1 ? DEFAULT_CHUNK_SIZE : chunkSize;
        int overlap = chunkOverlap == null || chunkOverlap < 0 ? DEFAULT_CHUNK_OVERLAP : chunkOverlap;
        if (overlap >= size) {
            throw new IllegalArgumentException("chunk_overlap必须小于chunk_size");
        }
        String normalized = normalize(content);
        List<ChunkPreviewVO> result = new ArrayList<>();
        if (oConvertUtils.isEmpty(normalized)) {
            return result;
        }
        int start = 0;
        int sortNo = 1;
        int length = normalized.length();
        while (start < length) {
            int end = Math.min(start + size, length);
            if (end < length) {
                int cutIndex = findCutIndex(normalized, start, end);
                if (cutIndex > start) {
                    end = cutIndex;
                }
            }
            String chunk = normalized.substring(start, end).trim();
            if (oConvertUtils.isNotEmpty(chunk)) {
                ChunkPreviewVO vo = new ChunkPreviewVO();
                vo.setContent(chunk);
                vo.setSortNo(sortNo++);
                vo.setTokenCount(chunk.length());
                result.add(vo);
            }
            if (end >= length) {
                break;
            }
            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }
        return result;
    }

    /**
     * 将确认导入请求的分段列表转成预览结果。
     *
     * @param chunks 分段项
     * @return chunk预览列表
     */
    public List<ChunkPreviewVO> toPreviewList(List<ImportConfirmDTO.ChunkItem> chunks) {
        List<ChunkPreviewVO> result = new ArrayList<>();
        if (chunks == null || chunks.isEmpty()) {
            return result;
        }
        for (ImportConfirmDTO.ChunkItem item : chunks) {
            if (item == null) {
                throw new IllegalArgumentException("chunks中存在空分段");
            }
            if (oConvertUtils.isEmpty(item.getContent())) {
                throw new IllegalArgumentException("分段内容不能为空");
            }
            ChunkPreviewVO vo = new ChunkPreviewVO();
            vo.setContent(item.getContent().trim());
            vo.setSortNo(item.getSortNo());
            vo.setTokenCount(item.getTokenCount() == null ? item.getContent().length() : item.getTokenCount());
            result.add(vo);
        }
        result.sort((left, right) -> {
            int leftSort = left.getSortNo() == null ? 0 : left.getSortNo();
            int rightSort = right.getSortNo() == null ? 0 : right.getSortNo();
            return Integer.compare(leftSort, rightSort);
        });
        return result;
    }

    /**
     * 规范化文本。
     *
     * @param content 文本内容
     * @return 规范化后的文本
     */
    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * 查找本轮切分位置。
     *
     * @param content 文本内容
     * @param start 起始位置
     * @param end 结束位置
     * @return 切分位置
     */
    private int findCutIndex(String content, int start, int end) {
        for (String separator : SEPARATORS) {
            if (oConvertUtils.isEmpty(separator)) {
                continue;
            }
            int index = content.lastIndexOf(separator, end - 1);
            if (index > start) {
                return index + separator.length();
            }
        }
        return end;
    }
}
