package org.jeecg.modules.system.util;

import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneImageGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStorySceneImagePromptOptimizeDto;
import org.jeecg.modules.system.dto.tsstory.TsStorySceneOptionDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorySceneOptionPromptTest {

    @Test
    void normalizesSceneOptionsAndBuildsImageVariables() {
        TsStoryOneClickSceneImageGenerateDto dto = new TsStoryOneClickSceneImageGenerateDto();
        dto.setSiteSetting("A quiet lakeside village");

        TsStorySceneOptionDto time = new TsStorySceneOptionDto();
        time.setKey(" day ");
        time.setDescription(" Bright natural daylight with clear visual layers ");
        dto.setTime(time);

        TsStorySceneOptionDto weather = new TsStorySceneOptionDto();
        weather.setKey("rain");
        weather.setDescription("Light rain, wet ground and soft diffused light");
        dto.setWeather(weather);

        dto.normalize();

        Map<String, String> variables = StoryPromptGenerateUtil.buildSceneImageVars(dto);

        assertEquals("day", dto.getTime().getKey());
        assertEquals("Bright natural daylight with clear visual layers", dto.getTime().getDescription());
        assertEquals("day", variables.get("time_key"));
        assertEquals("Light rain, wet ground and soft diffused light", variables.get("weather_description"));
        assertEquals("null", variables.get("mood_description"));
        assertTrue(dto.hasSceneContext());
    }

    @Test
    void dropsUnsupportedSceneOptionKeys() {
        TsStoryOneClickSceneImageGenerateDto dto = new TsStoryOneClickSceneImageGenerateDto();
        TsStorySceneOptionDto time = new TsStorySceneOptionDto();
        time.setKey("sun");
        time.setDescription("Unsupported time");
        dto.setTime(time);

        dto.normalize();

        assertNull(dto.getTime());
    }

    @Test
    void mapsSceneOptionsToPromptOptimizationVariables() {
        TsStorySceneImagePromptOptimizeDto dto = new TsStorySceneImagePromptOptimizeDto();
        dto.setPromptText("A moonlit forest path");

        TsStorySceneOptionDto mood = new TsStorySceneOptionDto();
        mood.setKey("mystic");
        mood.setDescription("Mysterious atmosphere with subtle, enigmatic lighting");
        dto.setMood(mood);
        dto.normalize();

        Map<String, String> variables =
                StoryPromptGenerateUtil.buildSceneImagePromptOptimizeVars(dto);

        assertEquals("A moonlit forest path", variables.get("prompt_text"));
        assertEquals("mystic", variables.get("mood_key"));
        assertEquals(
                "Mysterious atmosphere with subtle, enigmatic lighting",
                variables.get("mood_description")
        );
    }
}
