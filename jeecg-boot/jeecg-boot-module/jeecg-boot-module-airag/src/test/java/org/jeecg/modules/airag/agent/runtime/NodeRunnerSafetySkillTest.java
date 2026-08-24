package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.safety.GlobalSafetySkillPromptProvider;
import org.jeecg.modules.airag.agent.skill.registry.SkillRegistry;
import org.jeecg.modules.airag.safety.moderation.ModerationGuard;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class NodeRunnerSafetySkillTest {

    @Test
    void shouldForceLoadSafetySkillBeforeExecutingLlmNode() {
        AgentEventPublisher eventPublisher = Mockito.mock(AgentEventPublisher.class);
        SkillRegistry skillRegistry = Mockito.mock(SkillRegistry.class);
        Mockito.when(skillRegistry.getSkillBody(Mockito.anyString())).thenAnswer(invocation -> {
            String skillCode = invocation.getArgument(0);
            if (GlobalSafetySkillPromptProvider.SKILL_CODE.equals(skillCode)) {
                return "---\ncode: ai_safety_guard\n---\n\n# 安全规则\n\n始终遵守。";
            }
            return "";
        });
        GlobalSafetySkillPromptProvider safetyPromptProvider =
                new GlobalSafetySkillPromptProvider(skillRegistry);
        ModerationGuard moderationGuard = Mockito.mock(ModerationGuard.class);
        NodeRunner runner = new NodeRunner(eventPublisher, skillRegistry, safetyPromptProvider, moderationGuard);
        CapturingLlmNode node = new CapturingLlmNode();
        AgentContext context = new AgentContext();

        runner.run(context, node);

        Assertions.assertTrue(node.executed);
        Assertions.assertEquals("# 安全规则\n\n始终遵守。", context.getAttribute("safetySkillPrompt"));
        Assertions.assertTrue(((List<?>) context.getAttribute("loadedNodeSkillCodes"))
                .contains(GlobalSafetySkillPromptProvider.SKILL_CODE));
        Assertions.assertSame(moderationGuard, context.getAttribute("moderationGuard"));
    }

    @Test
    void shouldNotExecuteLlmNodeWhenSafetySkillIsMissing() {
        AgentEventPublisher eventPublisher = Mockito.mock(AgentEventPublisher.class);
        SkillRegistry skillRegistry = Mockito.mock(SkillRegistry.class);
        Mockito.when(skillRegistry.getSkillBody(GlobalSafetySkillPromptProvider.SKILL_CODE))
                .thenThrow(new JeecgBootException("未找到Skill"));
        GlobalSafetySkillPromptProvider safetyPromptProvider =
                new GlobalSafetySkillPromptProvider(skillRegistry);
        NodeRunner runner = new NodeRunner(
                eventPublisher, skillRegistry, safetyPromptProvider, Mockito.mock(ModerationGuard.class)
        );
        CapturingLlmNode node = new CapturingLlmNode();

        JeecgBootException exception = Assertions.assertThrows(
                JeecgBootException.class,
                () -> runner.run(new AgentContext(), node)
        );

        Assertions.assertFalse(node.executed);
        Assertions.assertTrue(exception.getMessage().contains("禁止执行LLM节点"));
    }

    @Test
    void shouldNotExecuteLlmNodeWhenContextIsMissing() {
        AgentEventPublisher eventPublisher = Mockito.mock(AgentEventPublisher.class);
        SkillRegistry skillRegistry = Mockito.mock(SkillRegistry.class);
        GlobalSafetySkillPromptProvider safetyPromptProvider =
                new GlobalSafetySkillPromptProvider(skillRegistry);
        NodeRunner runner = new NodeRunner(
                eventPublisher, skillRegistry, safetyPromptProvider, Mockito.mock(ModerationGuard.class)
        );
        CapturingLlmNode node = new CapturingLlmNode();

        JeecgBootException exception = Assertions.assertThrows(
                JeecgBootException.class,
                () -> runner.run(null, node)
        );

        Assertions.assertFalse(node.executed);
        Assertions.assertTrue(exception.getMessage().contains("上下文为空"));
    }

    @Test
    void shouldNotExecuteLlmNodeWhenModerationGuardIsMissing() {
        AgentEventPublisher eventPublisher = Mockito.mock(AgentEventPublisher.class);
        SkillRegistry skillRegistry = Mockito.mock(SkillRegistry.class);
        Mockito.when(skillRegistry.getSkillBody(GlobalSafetySkillPromptProvider.SKILL_CODE))
                .thenReturn("# 安全规则");
        GlobalSafetySkillPromptProvider safetyPromptProvider =
                new GlobalSafetySkillPromptProvider(skillRegistry);
        NodeRunner runner = new NodeRunner(eventPublisher, skillRegistry, safetyPromptProvider, null);
        CapturingLlmNode node = new CapturingLlmNode();

        JeecgBootException exception = Assertions.assertThrows(
                JeecgBootException.class,
                () -> runner.run(new AgentContext(), node)
        );

        Assertions.assertFalse(node.executed);
        Assertions.assertTrue(exception.getMessage().contains("审核服务不可用"));
    }

    private static class CapturingLlmNode extends LlmNode {
        private boolean executed;

        CapturingLlmNode() {
            super("safety_test_llm", "安全测试节点", buildDefinition(),
                    null, null, null, null);
        }

        @Override
        public NodeResult execute(AgentContext context) {
            this.executed = true;
            return NodeResult.success("ok");
        }

        @Override
        protected Map<String, String> buildPromptVariables(AgentContext context) {
            return Map.of();
        }

        @Override
        protected NodeResult parseResult(String finalText, AgentContext context) {
            return NodeResult.success(finalText);
        }

        private static LlmNodeDefinition buildDefinition() {
            LlmNodeDefinition definition = new LlmNodeDefinition();
            definition.setSystemPromptTemplate("系统提示");
            definition.setUserPromptTemplate("用户提示");
            return definition;
        }
    }
}
