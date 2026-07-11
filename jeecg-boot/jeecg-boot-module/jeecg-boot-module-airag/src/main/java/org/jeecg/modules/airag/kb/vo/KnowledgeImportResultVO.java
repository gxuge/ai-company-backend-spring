package org.jeecg.modules.airag.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库导入结果。
 */
@Data
@Schema(description = "知识库导入结果")
public class KnowledgeImportResultVO {
    /**
     * 文档信息。
     */
    @Schema(description = "文档信息")
    private KbDocumentVo document;

    /**
     * chunk列表。
     */
    @Schema(description = "chunk列表")
    private List<KbChunkVo> chunkList = new ArrayList<>();

    /**
     * chunk数量。
     */
    @Schema(description = "chunk数量")
    private Integer chunkCount;

    /**
     * 由文档和chunk列表构建返回结果。
     *
     * @param document 文档信息
     * @param chunkList chunk列表
     * @return 返回对象
     */
    public static KnowledgeImportResultVO from(KbDocumentVo document, List<KbChunkVo> chunkList) {
        KnowledgeImportResultVO vo = new KnowledgeImportResultVO();
        vo.setDocument(document);
        if (chunkList != null) {
            vo.setChunkList(chunkList);
            vo.setChunkCount(chunkList.size());
        } else {
            vo.setChunkCount(0);
        }
        return vo;
    }
}
