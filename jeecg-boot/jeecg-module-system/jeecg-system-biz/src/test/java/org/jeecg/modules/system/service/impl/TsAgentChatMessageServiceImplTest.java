package org.jeecg.modules.system.service.impl;

import org.jeecg.modules.system.entity.TsAgentChatMessage;
import org.jeecg.modules.system.entity.TsAgentChatSession;
import org.jeecg.modules.system.service.ITsAgentChatSessionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class TsAgentChatMessageServiceImplTest {

    @Test
    void shouldPersistAssistantMessageSourceFields() {
        ITsAgentChatSessionService sessionService = Mockito.mock(ITsAgentChatSessionService.class);
        TsAgentChatSession session = new TsAgentChatSession();
        session.setId(2001L);
        session.setAgentCode("main");
        Mockito.when(sessionService.getOwnedSession("user-1", 2001L)).thenReturn(session);

        TsAgentChatMessageServiceImpl service = Mockito.spy(new TsAgentChatMessageServiceImpl(sessionService));
        Mockito.doReturn(3L).when(service).nextMessageNo(2001L);
        Mockito.doReturn(true).when(service).save(Mockito.any(TsAgentChatMessage.class));

        service.saveAssistantMessage(
                "user-1",
                2001L,
                "sub_agent",
                "role_task_agent",
                "roleCreateDialogNode",
                "event-1001",
                "角色设定完成",
                "text",
                "success",
                1001L,
                "run-1",
                "role_dialog_v1",
                "app-1",
                null,
                null
        );

        ArgumentCaptor<TsAgentChatMessage> messageCaptor = ArgumentCaptor.forClass(TsAgentChatMessage.class);
        Mockito.verify(service).save(messageCaptor.capture());
        TsAgentChatMessage message = messageCaptor.getValue();
        Assertions.assertEquals("roleCreateDialogNode", message.getSourceNodeName());
        Assertions.assertEquals("event-1001", message.getSourceEventId());
        Assertions.assertEquals("sub_agent", message.getSenderType());
        Assertions.assertEquals("role_task_agent", message.getAgentCode());
    }

    @Test
    void shouldCompleteExistingAssistantMessage() {
        ITsAgentChatSessionService sessionService = Mockito.mock(ITsAgentChatSessionService.class);
        TsAgentChatSession session = new TsAgentChatSession();
        session.setId(2001L);
        session.setAgentCode("main");
        Mockito.when(sessionService.getOwnedSession("user-1", 2001L)).thenReturn(session);

        TsAgentChatMessage existing = new TsAgentChatMessage();
        existing.setId(3001L);
        existing.setSessionId(2001L);
        existing.setRoleType("assistant");
        existing.setMessageStatus("streaming");

        TsAgentChatMessageServiceImpl service = Mockito.spy(new TsAgentChatMessageServiceImpl(sessionService));
        Mockito.doReturn(existing).when(service).getOwnedMessage("user-1", 3001L);
        Mockito.doReturn(true).when(service).updateById(Mockito.any(TsAgentChatMessage.class));

        TsAgentChatMessage completed = service.completeAssistantMessage(
                "user-1",
                3001L,
                "sub_agent",
                "role_task_agent",
                "role_create_dialog",
                "event-1",
                "角色创建完成",
                "success",
                "role_dialog_v1",
                "model-1",
                null,
                null
        );

        Assertions.assertSame(existing, completed);
        Assertions.assertEquals("角色创建完成", completed.getContent());
        Assertions.assertEquals("success", completed.getMessageStatus());
        Assertions.assertEquals("role_create_dialog", completed.getSourceNodeName());
        Mockito.verify(sessionService).touchAfterMessage(2001L, 3001L, completed.getUpdatedAt(), 0, 0);
    }
}
