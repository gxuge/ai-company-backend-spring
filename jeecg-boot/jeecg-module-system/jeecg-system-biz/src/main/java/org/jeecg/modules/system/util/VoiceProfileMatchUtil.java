package org.jeecg.modules.system.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.mapper.TsVoiceProfileMapper;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileQueryDto;
import org.jeecg.modules.system.po.tsvoiceprofile.TsVoiceProfileQueryPo;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 音色匹配工具类。
 * 用途：根据推荐关键词在候选音色中做优先匹配，并提供稳定兜底策略。
 */
public class VoiceProfileMatchUtil {
    private VoiceProfileMatchUtil() {
    }

    /**
     * 优先在主候选列表中匹配关键词，失败后回退到备选列表。
     */
    public static TsVoiceProfile selectBestVoiceProfile(List<TsVoiceProfile> primaryProfiles,
                                                        List<TsVoiceProfile> fallbackProfiles,
                                                        String preferredVoiceName) {
        TsVoiceProfile matched = matchByName(primaryProfiles, preferredVoiceName);
        if (matched != null) return matched;
        if (primaryProfiles != null && !primaryProfiles.isEmpty()) return primaryProfiles.get(0);

        matched = matchByName(fallbackProfiles, preferredVoiceName);
        if (matched != null) return matched;
        if (fallbackProfiles != null && !fallbackProfiles.isEmpty()) return fallbackProfiles.get(0);
        return null;
    }

    /**
     * 查询音色列表，按性别可选过滤。
     */
    public static List<TsVoiceProfile> queryVoiceProfiles(TsVoiceProfileMapper tsVoiceProfileMapper, String gender, int pageSize) {
        TsVoiceProfileQueryDto dto = new TsVoiceProfileQueryDto();
        dto.setPageNo(1);
        dto.setPageSize(pageSize);
        dto.setGender(gender);
        TsVoiceProfileQueryPo po = TsVoiceProfileQueryPo.fromRequest(dto);
        Page<TsVoiceProfile> page = tsVoiceProfileMapper.selectVoiceProfilePage(new Page<>(1, pageSize), po);
        return page == null || page.getRecords() == null ? new ArrayList<>() : page.getRecords();
    }

    /**
     * 按“包含关系”匹配音色名关键词。
     */
    private static TsVoiceProfile matchByName(List<TsVoiceProfile> profiles, String keyword) {
        if (!StringUtils.hasText(keyword) || profiles == null) return null;
        String normalized = keyword.trim().toLowerCase();
        for (TsVoiceProfile profile : profiles) {
            if (profile != null && StringUtils.hasText(profile.getName())
                    && profile.getName().toLowerCase().contains(normalized)) {
                return profile;
            }
        }
        return null;
    }
}
