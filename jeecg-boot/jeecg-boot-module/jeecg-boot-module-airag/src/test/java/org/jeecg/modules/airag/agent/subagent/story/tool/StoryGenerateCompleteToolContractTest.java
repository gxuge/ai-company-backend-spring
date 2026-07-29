package org.jeecg.modules.airag.agent.subagent.story.tool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class StoryGenerateCompleteToolContractTest {

    @Test
    void shouldNormalizeStoryFieldsAndNewRoleList() {
        Map<String, Object> transferData = StoryGenerateCompleteToolContract.requireTransferData(Map.of(
                "transferData", Map.of(
                        "title", "夜航",
                        "storyMode", "normal",
                        "storyIntro", "失踪船队引出王国阴谋。",
                        "storySetting", "群岛组成的海洋王国。",
                        "siteSetting", "终年被浓雾笼罩的雾港。",
                        "plotOutline", "骑士逐步发现王室阴谋。",
                        "roles", List.of(
                                Map.of(
                                        "roleName", "艾琳",
                                        "gender", "female",
                                        "occupation", "魔法师",
                                        "backgroundStory", "来自北境魔法学院。",
                                        "ignoredField", "不应传递"
                                )
                        )
                )
        ));

        Assertions.assertEquals("夜航", transferData.get("title"));
        Assertions.assertEquals(List.of(Map.of(
                "roleName", "艾琳",
                "gender", "female",
                "occupation", "魔法师",
                "backgroundStory", "来自北境魔法学院。"
        )), transferData.get("roles"));
    }

    @Test
    void shouldRejectRoleWithoutRequiredField() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> StoryGenerateCompleteToolContract.requireTransferData(Map.of(
                        "transferData", Map.of(
                                "title", "夜航",
                                "storyMode", "normal",
                                "storyIntro", "失踪船队引出王国阴谋。",
                                "storySetting", "群岛组成的海洋王国。",
                                "siteSetting", "终年被浓雾笼罩的雾港。",
                                "plotOutline", "骑士逐步发现王室阴谋。",
                                "roles", List.of(Map.of(
                                        "roleName", "艾琳",
                                        "gender", "female",
                                        "occupation", "魔法师",
                                        "backgroundStory", " "
                                ))
                        )
                ))
        );

        Assertions.assertEquals(
                "transferData.roles[0].backgroundStory 不能为空",
                exception.getMessage()
        );
    }
}
