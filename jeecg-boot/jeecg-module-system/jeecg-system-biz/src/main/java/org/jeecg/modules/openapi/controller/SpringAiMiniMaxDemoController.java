package org.jeecg.modules.openapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.openapi.dto.MiniMaxChatRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxImageRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxTtsRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.service.impl.MiniMaxDemoGuardService;
import org.jeecg.modules.openapi.vo.MiniMaxChatResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxTtsResponseVo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MiniMax demo controller.
 */
@Tag(name = "MiniMax Demo API")
@RestController
@RequestMapping("/ai/minimax")
@ConditionalOnProperty(prefix = "jeecg.airag.minimax.demo", name = "enabled", havingValue = "true")
public class SpringAiMiniMaxDemoController {

    private final IMiniMaxDemoService miniMaxDemoService;
    private final MiniMaxDemoGuardService miniMaxDemoGuardService;

    public SpringAiMiniMaxDemoController(IMiniMaxDemoService miniMaxDemoService,
                                         MiniMaxDemoGuardService miniMaxDemoGuardService) {
        this.miniMaxDemoService = miniMaxDemoService;
        this.miniMaxDemoGuardService = miniMaxDemoGuardService;
    }

    /**
     * Chat API.
     */
    @Operation(summary = "MiniMax text chat")
    @PostMapping("/chat")
    public Result<MiniMaxChatResponseVo> chat(@Valid @RequestBody MiniMaxChatRequestDto requestDto,
                                              HttpServletRequest request) {
        miniMaxDemoGuardService.checkRequest(request, "chat");
        return Result.OK(miniMaxDemoService.chat(requestDto));
    }

    /**
     * Text to speech API.
     */
    @Operation(summary = "MiniMax text to speech")
    @PostMapping("/tts")
    public Result<MiniMaxTtsResponseVo> tts(@Valid @RequestBody MiniMaxTtsRequestDto requestDto,
                                            HttpServletRequest request) {
        miniMaxDemoGuardService.checkRequest(request, "tts");
        return Result.OK(miniMaxDemoService.tts(requestDto));
    }

    /**
     * Image generation API.
     */
    @Operation(summary = "MiniMax text to image")
    @PostMapping("/image")
    public Result<MiniMaxImageResponseVo> image(@Valid @RequestBody MiniMaxImageRequestDto requestDto,
                                                 HttpServletRequest request) {
        miniMaxDemoGuardService.checkRequest(request, "image");
        return Result.OK(miniMaxDemoService.image(requestDto));
    }
}
