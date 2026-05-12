package org.jeecg.modules.airag.llm.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.ai.factory.AiModelFactory;
import org.jeecg.ai.factory.AiModelOptions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.AssertUtils;
import org.jeecg.common.util.TokenUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.config.mybatis.MybatisPlusSaasConfig;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.llm.consts.LLMConsts;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.jeecg.modules.airag.llm.handler.AIChatHandler;
import org.jeecg.modules.airag.llm.handler.EmbeddingHandler;
import org.jeecg.modules.airag.llm.service.IAiragModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

/**
 * @Description: AiRag模型配置
 * @Author: jeecg-boot
 * @Date: 2025-02-14
 * @Version: V1.0
 */
@Tag(name = "AiRag模型配置")
@RestController
@RequestMapping("/airag/airagModel")
@Slf4j
public class AiragModelController extends JeecgController<AiragModel, IAiragModelService> {
    private static final String DEFAULT_MINIMAX_VOICE_ID = "Chinese (Mandarin)_Wise_Women";

    @Autowired
    private IAiragModelService airagModelService;

    @Autowired
    AIChatHandler aiChatHandler;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 分页列表查询
     *
     * @param airagModel
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping(value = "/list")
    public Result<IPage<AiragModel>> queryPageList(AiragModel airagModel, @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo, @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize, HttpServletRequest req) {
        QueryWrapper<AiragModel> queryWrapper = QueryGenerator.initQueryWrapper(airagModel, req.getParameterMap());
        Page<AiragModel> page = new Page<AiragModel>(pageNo, pageSize);
        IPage<AiragModel> pageList = airagModelService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param airagModel
     * @return
     */
    @PostMapping(value = "/add")
    @RequiresPermissions("airag:model:add")
    public Result<String> add(@RequestBody AiragModel airagModel) {
        // 验证 模型名称/模型类型/基础模型
        AssertUtils.assertNotEmpty("模型名称不能为空", airagModel.getName());
        AssertUtils.assertNotEmpty("模型类型不能为空", airagModel.getModelType());
        AssertUtils.assertNotEmpty("基础模型不能为空", airagModel.getModelName());
        // 默认未激活
        if(oConvertUtils.isObjectEmpty(airagModel.getActivateFlag())){
            airagModel.setActivateFlag(0);
        } else {
            airagModel.setActivateFlag(1);
        }
        airagModelService.save(airagModel);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param airagModel
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresPermissions("airag:model:edit")
    public Result<String> edit(@RequestBody AiragModel airagModel) {
        airagModelService.updateById(airagModel);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    @RequiresPermissions("airag:model:delete")
    public Result<String> delete(HttpServletRequest request, @RequestParam(name = "id", required = true) String id) {
        //update-begin---author:chenrui ---date:20250606  for：[issues/8337]关于ai工作列表的数据权限问题 #8337------------
        //如果是saas隔离的情况下，判断当前租户id是否是当前租户下的
        if (MybatisPlusSaasConfig.OPEN_SYSTEM_TENANT_CONTROL) {
            AiragModel model = airagModelService.getById(id);
            //获取当前租户
            String currentTenantId = TokenUtils.getTenantIdByRequest(request);
            if (null == model || !model.getTenantId().equals(currentTenantId)) {
                return Result.error("删除AI模型失败，不能删除其他租户的AI模型！");
            }
        }
        //update-end---author:chenrui ---date:20250606  for：[issues/8337]关于ai工作列表的数据权限问题 #8337------------
        airagModelService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/queryById")
    public Result<AiragModel> queryById(@RequestParam(name = "id", required = true) String id) {
        AiragModel airagModel = airagModelService.getById(id);
        if (airagModel == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(airagModel);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param airagModel
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, AiragModel airagModel) {
        return super.exportXls(request, airagModel, AiragModel.class, "AiRag模型配置");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, AiragModel.class);
    }

    @PostMapping(value = "/test")
    public Result<?> test(@RequestBody AiragModel airagModel) {
        // 验证 模型名称/模型类型/基础模型
        AssertUtils.assertNotEmpty("模型名称不能为空", airagModel.getName());
        AssertUtils.assertNotEmpty("模型类型不能为空", airagModel.getModelType());
        AssertUtils.assertNotEmpty("基础模型不能为空", airagModel.getModelName());
        try {
            if(LLMConsts.MODEL_TYPE_LLM.equals(airagModel.getModelType())){
                aiChatHandler.completions(airagModel, Collections.singletonList(UserMessage.from("To test whether it can be successfully called, simply return success")), null);
            }else if(LLMConsts.MODEL_TYPE_EMBED.equals(airagModel.getModelType())){
                AiModelOptions aiModelOptions = EmbeddingHandler.buildModelOptions(airagModel);
                EmbeddingModel embeddingModel = AiModelFactory.createEmbeddingModel(aiModelOptions);
                embeddingModel.embed("test text");
            //update-begin---author:wangshuai---date:2026-01-07---for:【QQYUN-12145】【AI】AI 绘画创作---=
            }else if(LLMConsts.MODEL_TYPE_IMAGE.equals(airagModel.getModelType())){
                AIChatParams aiChatParams = new AIChatParams();
                aiChatHandler.imageGenerate(airagModel, "To test whether it can be successfully called, simply return success", aiChatParams);
            }else if(LLMConsts.MODEL_TYPE_VOICE.equals(airagModel.getModelType())){
                this.testVoiceModel(airagModel);
            }else{
                throw new IllegalArgumentException("不支持的模型类型: " + airagModel.getModelType());
            }
            //update-end---author:wangshuai---date:2026-01-07---for:【QQYUN-12145】【AI】AI 绘画创作---
        }catch (Exception e){
            log.error("测试模型连接失败", e);
            return Result.error(e.getMessage());
        }
        // 测试成功激活数据
        airagModel.setActivateFlag(1);
        airagModelService.updateById(airagModel);
        return Result.OK("");
    }

    /**
     * VOICE 模型健康检查（当前按 MiniMax TTS 调用）。
     * 说明：为避免模块间编译依赖，这里通过反射调用 IMiniMaxDemoService#tts。
     */
    private void testVoiceModel(AiragModel airagModel) throws Exception {
        String provider = airagModel.getProvider();
        if (!"MINIMAX".equalsIgnoreCase(provider)) {
            throw new IllegalArgumentException("VOICE 模型当前仅支持 provider=MINIMAX，当前为: " + provider);
        }

        JSONObject modelParams = null;
        if (oConvertUtils.isObjectNotEmpty(airagModel.getModelParams())) {
            modelParams = JSONObject.parseObject(airagModel.getModelParams());
        }
        String voiceId = modelParams == null ? null : modelParams.getString("voiceId");
        if (oConvertUtils.isEmpty(voiceId) && modelParams != null) {
            voiceId = modelParams.getString("providerVoiceId");
        }
        if (oConvertUtils.isEmpty(voiceId)) {
            voiceId = DEFAULT_MINIMAX_VOICE_ID;
        }
        if (oConvertUtils.isEmpty(voiceId)) {
            throw new IllegalArgumentException("VOICE 模型测试失败：请在 modelParams 中配置 voiceId 或 providerVoiceId");
        }

        Double speed = modelParams == null ? null : modelParams.getDouble("speed");
        Double pitch = modelParams == null ? null : modelParams.getDouble("pitch");
        Double volume = modelParams == null ? null : modelParams.getDouble("volume");

        Class<?> serviceType = Class.forName("org.jeecg.modules.openapi.service.IMiniMaxDemoService");
        Object miniMaxDemoService = applicationContext.getBean(serviceType);
        if (miniMaxDemoService == null) {
            throw new IllegalStateException("未找到 IMiniMaxDemoService，请确认 MiniMax 服务已启用");
        }

        Class<?> requestType = Class.forName("org.jeecg.modules.openapi.dto.MiniMaxTtsRequestDto");
        Object request = requestType.getDeclaredConstructor().newInstance();
        requestType.getMethod("setText", String.class).invoke(request, "VOICE_HEALTH_CHECK");
        requestType.getMethod("setVoiceId", String.class).invoke(request, voiceId);
        if (speed != null) {
            requestType.getMethod("setSpeed", Double.class).invoke(request, speed);
        }
        if (pitch != null) {
            requestType.getMethod("setPitch", Double.class).invoke(request, pitch);
        }
        if (volume != null) {
            requestType.getMethod("setVolume", Double.class).invoke(request, volume);
        }

        Method ttsMethod = serviceType.getMethod("tts", requestType);
        Object response = ttsMethod.invoke(miniMaxDemoService, request);
        if (response == null) {
            throw new IllegalStateException("VOICE 模型测试失败：TTS 返回为空");
        }

        String audioUrl = toStringSafely(response.getClass().getMethod("getAudioUrl").invoke(response));
        String audioHex = toStringSafely(response.getClass().getMethod("getAudioHex").invoke(response));
        if (oConvertUtils.isEmpty(audioUrl) && oConvertUtils.isEmpty(audioHex)) {
            throw new IllegalStateException("VOICE 模型测试失败：TTS 未返回 audioUrl/audioHex");
        }
    }

    private String toStringSafely(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

}
