package org.jeecg.modules.system.agent.task.role;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorException;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleGenerateCompleteToolContract;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
import org.jeecg.modules.airag.agent.task.TaskAgentSupport;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.system.dto.tsrole.TsRoleConfirmedGenerateDto;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 完整角色后台生成服务。
 */
@Service
@RequiredArgsConstructor
public class RoleGenerateCompleteAsyncService {
    private static final String DEFAULT_NODE_NAME = "role_create_dialog";

    private final ITsRoleGenerateService roleGenerateService;
    private final AgentEventPublisher eventPublisher;

    /**
     * 提交完整角色生成任务并立即返回受理结果。
     */
    public ToolCallResult submit(AgentContext context,
                                 ToolCallRequest request,
                                 Map<String, Object> transferData) {
        String taskId = request == null ? null : request.getTaskId();
        String eventId = request == null ? null : request.getEventId();
        if (taskId == null || taskId.isBlank() || eventId == null || eventId.isBlank()) {
            throw new AgentErrorException(
                    AgentErrorCode.TOOL_ROLE_GENERATION_REQUIRED_FIELD_MISSING,
                    Map.of("field", "taskId/eventId")
            );
        }

        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        AgentContext asyncContext = context == null ? new AgentContext() : context.fork(context.getUserInput());
        String nodeName = context == null || context.getCurrentNodeName() == null
                ? DEFAULT_NODE_NAME
                : context.getCurrentNodeName();
        TsRoleConfirmedGenerateDto dto = buildGenerateRequest(transferData);

        CompletableFuture.runAsync(() -> executeGeneration(
                asyncContext,
                user,
                dto,
                taskId,
                eventId,
                nodeName,
                transferData
        ));

        String transferDataJson = JSON.toJSONString(transferData);
        RoleGenerateCompleteToolContract.markAccepted(context, taskId, eventId, transferDataJson);

        Map<String, Object> accepted = new LinkedHashMap<>();
        accepted.put("taskId", taskId);
        accepted.put("eventId", eventId);
        accepted.put("status", "running");
        ToolCallResult result = ToolCallResult.asyncAccepted(
                "Role generation started",
                taskId,
                eventId,
                accepted
        );
        result.setPayload(new LinkedHashMap<>(accepted));
        return result;
    }

    private void executeGeneration(AgentContext context,
                                   LoginUser user,
                                   TsRoleConfirmedGenerateDto request,
                                   String taskId,
                                   String eventId,
                                   String nodeName,
                                   Map<String, Object> transferData) {
        try {
            TsRoleGenerateRoleVo result = this.roleGenerateService.generateConfirmedRole(user, request);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("taskId", taskId);
            payload.put("eventId", eventId);
            payload.put("async", Boolean.TRUE);
            payload.put("toolArguments", Map.of(RoleGenerateCompleteToolContract.TRANSFER_DATA, transferData));
            payload.put("toolData", result);
            this.eventPublisher.publishAsyncToolEnd(
                    context,
                    eventId,
                    nodeName,
                    RoleTaskToolSpec.ROLE_GENERATE_COMPLETE,
                    "Role generation completed",
                    payload
            );
        } catch (Exception ex) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("taskId", taskId);
            payload.put("eventId", eventId);
            payload.put("async", Boolean.TRUE);
            payload.put("toolArguments", Map.of(RoleGenerateCompleteToolContract.TRANSFER_DATA, transferData));
            this.eventPublisher.publishAsyncToolError(
                    context,
                    eventId,
                    nodeName,
                    RoleTaskToolSpec.ROLE_GENERATE_COMPLETE,
                    ex,
                    payload
            );
        }
    }

    private TsRoleConfirmedGenerateDto buildGenerateRequest(Map<String, Object> transferData) {
        TsRoleConfirmedGenerateDto request = new TsRoleConfirmedGenerateDto();
        request.setRoleName(stringValue(transferData.get("roleName")));
        request.setGender(stringValue(transferData.get("gender")));
        request.setOccupation(stringValue(transferData.get("occupation")));
        request.setBackgroundStory(stringValue(transferData.get("backgroundStory")));
        request.normalize();
        return request;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
