package com.sait.peelin.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sait.peelin.dto.v1.StaffConversationDto;
import com.sait.peelin.dto.v1.StaffMessageDto;
import com.sait.peelin.service.StaffMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = StaffMessageController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ClientWebSecurityAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
            pattern = "com\\.sait\\.peelin\\.security\\..*")
)
@AutoConfigureMockMvc(addFilters = false)
class StaffMessageControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean StaffMessageService staffMessageService;

    UUID otherUser = UUID.randomUUID();

    StaffConversationDto sampleConvo() {
        return new StaffConversationDto(1, otherUser, "bob", OffsetDateTime.now(), 0);
    }

    StaffMessageDto sampleMsg() {
        return new StaffMessageDto(1, 1, UUID.randomUUID(), "hello", OffsetDateTime.now(), false);
    }

    @Test
    void listConversations_Returns200() throws Exception {
        when(staffMessageService.conversations()).thenReturn(List.of(sampleConvo()));

        mockMvc.perform(get("/api/v1/messages/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void startConversation_Returns201() throws Exception {
        when(staffMessageService.getOrCreateConversation(otherUser)).thenReturn(sampleConvo());

        mockMvc.perform(post("/api/v1/messages/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientId\":\"" + otherUser + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otherUsername").value("bob"));
    }

    @Test
    void getMessages_Returns200() throws Exception {
        when(staffMessageService.messages(1)).thenReturn(List.of(sampleMsg()));

        mockMvc.perform(get("/api/v1/messages/conversations/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("hello"));
    }

    @Test
    void sendMessage_Returns201() throws Exception {
        when(staffMessageService.sendMessage(eq(1), eq("hello"))).thenReturn(sampleMsg());

        mockMvc.perform(post("/api/v1/messages/conversations/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("hello"));
    }

    @Test
    void markRead_Returns204() throws Exception {
        doNothing().when(staffMessageService).markRead(1);

        mockMvc.perform(post("/api/v1/messages/conversations/1/read"))
                .andExpect(status().isNoContent());
    }
}
