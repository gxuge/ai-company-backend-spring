package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsworkreview.TsWorkReviewActionDto;
import org.jeecg.modules.system.dto.tsworkreview.TsWorkReviewQueryDto;
import org.jeecg.modules.system.entity.TsWorkReview;
import org.jeecg.modules.system.vo.tsworkreview.TsWorkReviewVo;

public interface ITsWorkReviewService {
    TsWorkReview submitRole(Long roleId, Integer requestedPublic);

    TsWorkReview submitStory(Long storyId, Integer requestedPublic);

    void runAiReview(Long reviewId);

    Result<TsWorkReviewVo> getCurrent(LoginUser user, String workType, Long workId);

    Result<Page<TsWorkReviewVo>> pageAdmin(TsWorkReviewQueryDto request);

    Result<TsWorkReviewVo> getAdminDetail(Long id);

    Result<TsWorkReviewVo> approve(LoginUser user, TsWorkReviewActionDto request);

    Result<TsWorkReviewVo> reject(LoginUser user, TsWorkReviewActionDto request);

    Result<TsWorkReviewVo> retryAi(TsWorkReviewActionDto request);
}
