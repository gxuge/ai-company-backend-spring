package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.system.dto.tscontenttag.TsContentTagCandidateDto;
import org.jeecg.modules.system.entity.TsContentTag;
import org.jeecg.modules.system.vo.tscontenttag.TsContentTagDisplayVo;

import java.util.List;
import java.util.Map;

/** 角色与故事内容标签服务。 */
public interface ITsContentTagService extends IService<TsContentTag> {

    /** 从模型 JSON 中读取候选标签。 */
    List<TsContentTagCandidateDto> parseCandidates(Object rawTags);

    /** 校验并按内容版本覆盖标签，返回实际保存数量。 */
    int replaceTags(String contentType, Long contentId, Integer contentVersion, String contentHash,
                    String source, String modelVersion, List<TsContentTagCandidateDto> candidates,
                    boolean allowNextVersion);

    /** 判断指定内容版本是否已有有效标签。 */
    boolean hasTags(String contentType, Long contentId, Integer contentVersion);

    /** 输出指定内容类型的启用标签词典 JSON。 */
    String buildDictionaryJson(String contentType);

    /** 按内容当前版本批量读取面向用户展示的标签。 */
    Map<Long, List<TsContentTagDisplayVo>> findCurrentDisplayTags(
            String contentType, Map<Long, Integer> contentVersions);
}
