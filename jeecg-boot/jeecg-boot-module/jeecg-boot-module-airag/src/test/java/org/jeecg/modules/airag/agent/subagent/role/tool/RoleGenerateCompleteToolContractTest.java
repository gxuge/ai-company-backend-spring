package org.jeecg.modules.airag.agent.subagent.role.tool;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class RoleGenerateCompleteToolContractTest {

    @Test
    void shouldKeepOnlyFourRequiredFields() {
        Map<String, Object> transferData = RoleGenerateCompleteToolContract.requireTransferData(Map.of(
                "transferData", Map.of(
                        "roleName", "亚瑟·雷恩哈特",
                        "gender", "男性",
                        "occupation", "皇家骑士",
                        "backgroundStory", "曾是最年轻的骑士团长。",
                        "ignoredField", "不应传递"
                )
        ));

        Assertions.assertEquals(Map.of(
                "roleName", "亚瑟·雷恩哈特",
                "gender", "男性",
                "occupation", "皇家骑士",
                "backgroundStory", "曾是最年轻的骑士团长。"
        ), transferData);
    }

    @Test
    void shouldMarkAndConsumeAcceptedState() {
        AgentContext context = new AgentContext();
        RoleGenerateCompleteToolContract.markAccepted(context, "task-1", "event-1", "{}");

        Assertions.assertTrue(RoleGenerateCompleteToolContract.consumeAccepted(context));
        Assertions.assertFalse(RoleGenerateCompleteToolContract.consumeAccepted(context));
        Assertions.assertEquals("task-1", context.getAttribute(
                RoleGenerateCompleteToolContract.ATTR_GENERATION_TASK_ID
        ));
    }

    @Test
    void shouldRejectWhenAnyRequiredFieldIsBlank() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RoleGenerateCompleteToolContract.requireTransferData(Map.of(
                        "transferData", Map.of(
                                "roleName", "亚瑟·雷恩哈特",
                                "gender", "男性",
                                "occupation", " ",
                                "backgroundStory", "曾是最年轻的骑士团长。"
                        )
                ))
        );

        Assertions.assertEquals("transferData.occupation 不能为空", exception.getMessage());
    }
}
