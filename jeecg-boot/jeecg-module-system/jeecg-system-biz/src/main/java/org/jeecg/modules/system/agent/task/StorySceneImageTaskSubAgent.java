package org.jeecg.modules.system.agent.task;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.openapi.dto.MiniMaxImageRequestDto;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 故事场景图任务子 Agent。
 *
 * @author codex
 * @date 2026/7/1
 */
@Component
public class StorySceneImageTaskSubAgent implements SubAgent {

    /**
     * 文生图服务。
     */
    private final IMiniMaxDemoService miniMaxDemoService;

    /**
     * 构造函数。
     *
     * @param miniMaxDemoService 文生图服务
     */
    public StorySceneImageTaskSubAgent(IMiniMaxDemoService miniMaxDemoService) {
        this.miniMaxDemoService = miniMaxDemoService;
    }

    @Override
    public String subAgentName() {
        return "story_scene_image_task_agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Map<String, String> promptVariables = TaskImageSupport.readPromptVariables(context);
        JSONObject memory = TaskImageSupport.readSessionMemory(context);
        Map<String, Object> routeDecision = TaskAgentSupport.readMapAttribute(context, "routeDecision");
        String userInput = TaskAgentSupport.normalizeText(context == null ? null : context.getUserInput());
        String taskGoal = TaskAgentSupport.normalizeText(stringValue(routeDecision.get("taskGoal")));

        Long storyId = TaskImageSupport.resolveLong(promptVariables, memory, "storyId", "story_id");
        String title = TaskImageSupport.resolveText(promptVariables, memory,
                PromptRuntimeUtil.firstNonBlank(taskGoal, userInput),
                "title");
        String storyIntro = TaskImageSupport.resolveText(promptVariables, memory, null, "story_intro", "storyIntro");
        String storySetting = TaskImageSupport.resolveText(promptVariables, memory, null, "story_setting", "storySetting");
        String siteSetting = TaskImageSupport.resolveText(promptVariables, memory, null, "site_setting", "siteSetting");
        String plotOutline = TaskImageSupport.resolveText(promptVariables, memory, null, "plot_outline", "plotOutline");
        String styleName = TaskImageSupport.resolveText(promptVariables, memory, null, "style_name", "styleName");
        String aspectRatio = TaskImageSupport.resolveText(promptVariables, memory, null, "aspect_ratio", "aspectRatio");
        String referenceImageUrl = TaskImageSupport.resolveText(promptVariables, memory, null, "reference_image_url", "referenceImageUrl");

        String imagePrompt = TaskImageSupport.buildStorySceneImagePrompt(
                title,
                storyIntro,
                storySetting,
                siteSetting,
                plotOutline,
                styleName,
                aspectRatio,
                referenceImageUrl,
                userInput,
                taskGoal
        );

        MiniMaxImageRequestDto imageRequest = new MiniMaxImageRequestDto();
        imageRequest.setPrompt(imagePrompt);
        MiniMaxImageResponseVo imageResponse = this.miniMaxDemoService.image(imageRequest);
        String imageUrl = TaskImageSupport.extractFirstImageUrl(imageResponse);
        if (!org.springframework.util.StringUtils.hasText(imageUrl)) {
            throw new JeecgBootException("故事场景图生成失败，未返回图片地址");
        }

        return buildResult(storyId, title, storyIntro, storySetting, siteSetting, plotOutline,
                styleName, aspectRatio, referenceImageUrl, imagePrompt, imageUrl, imageResponse);
    }

    /**
     * 组装故事场景图结果。
     */
    private AgentResult buildResult(Long storyId,
                                    String title,
                                    String storyIntro,
                                    String storySetting,
                                    String siteSetting,
                                    String plotOutline,
                                    String styleName,
                                    String aspectRatio,
                                    String referenceImageUrl,
                                    String imagePrompt,
                                    String imageUrl,
                                    MiniMaxImageResponseVo imageResponse) {
        AgentResult result = AgentResult.success("已生成故事场景图");
        result.getData().put("subAgentName", subAgentName());
        result.getData().put("executionMode", "image");
        result.getData().put("taskType", "story_scene_image");
        result.getData().put("storyId", storyId);
        result.getData().put("title", title);
        result.getData().put("storyIntro", storyIntro);
        result.getData().put("storySetting", storySetting);
        result.getData().put("siteSetting", siteSetting);
        result.getData().put("plotOutline", plotOutline);
        result.getData().put("styleName", styleName);
        result.getData().put("aspectRatio", aspectRatio);
        result.getData().put("referenceImageUrl", referenceImageUrl);
        result.getData().put("imagePrompt", imagePrompt);
        result.getData().put("imageUrl", imageUrl);
        result.getData().put("originalImageUrls", imageResponse == null ? null : imageResponse.getOriginalImageUrls());
        result.getData().put("result", imageResponse);
        result.getData().put("resultJson", JSONObject.toJSONString(imageResponse));
        return result;
    }

    /**
     * 将对象值转为文本。
     */
    private static String stringValue(Object value) {
        return value == null ? null : TaskAgentSupport.normalizeText(String.valueOf(value));
    }
}
