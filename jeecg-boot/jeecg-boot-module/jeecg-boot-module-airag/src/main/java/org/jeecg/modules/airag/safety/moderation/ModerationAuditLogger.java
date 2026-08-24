package org.jeecg.modules.airag.safety.moderation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * 脱敏审核日志记录器。
 */
@Slf4j
@Component
public class ModerationAuditLogger {

    /**
     * 记录审核元数据，不记录完整待审核原文。
     *
     * @param request 审核请求
     * @param result 审核结果
     */
    public void log(ModerationRequest request, ModerationResult result) {
        String content = request == null ? null : request.getContent();
        log.info(
                "ai_moderation stage={} scene={} category={} score={} action={} service={} "
                        + "contextReviewed={} contentLength={} contentHash={} requestId={} time={}",
                request == null ? null : request.getStage(),
                request == null ? null : request.getScene(),
                result == null ? null : result.getCategory(),
                result == null ? null : result.getScore(),
                result == null ? null : result.getAction(),
                result == null ? null : result.getModerationService(),
                result != null && result.isContextReviewed(),
                content == null ? 0 : content.codePointCount(0, content.length()),
                contentHash(content),
                request == null ? null : request.getRequestId(),
                Instant.now()
        );
    }

    /**
     * 生成不可逆内容摘要前缀。
     */
    private String contentHash(String content) {
        if (content == null || content.isEmpty()) {
            return "empty";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < Math.min(8, digest.length); i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (Exception ignored) {
            return "hash-error";
        }
    }
}
