package org.jeecg.modules.airag.agent.graph;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class DeepAgentDefinitionRegistryTest {

    @Test
    void shouldRegisterSingleNodeGenerationAgents() {
        DeepAgentDefinitionRegistry registry = new DeepAgentDefinitionRegistry();
        registry.init();

        DeepAgentDefinition roleImage = registry.require("role_image_task_agent");
        Assertions.assertEquals(List.of("role_create_image"), roleImage.getSkills());
        Assertions.assertEquals(List.of("role_generate_role_image"), roleImage.getTools());
        Assertions.assertEquals(List.of("role_create_image"), roleImage.getMetadata().get("chain"));

        DeepAgentDefinition storyBackground = registry.require("story_background_task_agent");
        Assertions.assertEquals(List.of("story_create_background"), storyBackground.getSkills());
        Assertions.assertEquals(List.of("story_generate_scene"), storyBackground.getTools());
        Assertions.assertEquals(List.of("story_create_background"), storyBackground.getMetadata().get("chain"));
    }
}
