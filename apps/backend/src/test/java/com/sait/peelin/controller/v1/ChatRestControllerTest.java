package com.sait.peelin.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.repository.EmployeeSpecialtyRepository;
import com.sait.peelin.service.ChatService;
import com.sait.peelin.service.CurrentUserService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = ChatRestController.class,
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
class ChatRestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ChatService chatService;
    @MockitoBean CurrentUserService currentUserService;
    @MockitoBean EmployeeSpecialtyRepository employeeSpecialtyRepository;

    ChatThreadDto thread(String status, String category) {
        return new ChatThreadDto(1, UUID.randomUUID(), "Alice", "alice",
                "alice@test.com", null, null, null, null, status, category,
                OffsetDateTime.now(), OffsetDateTime.now(), null);
    }

    @Test
    void createThread_WithCategory_CallsServiceWithCategory() throws Exception {
        when(chatService.createThread("order_issue")).thenReturn(thread("open", "order_issue"));

        mockMvc.perform(post("/api/v1/chat/threads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"order_issue\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("order_issue"));

        verify(chatService).createThread("order_issue");
    }

    @Test
    void createThread_NoBody_CallsServiceWithGeneral() throws Exception {
        when(chatService.createThread("general")).thenReturn(thread("open", "general"));

        mockMvc.perform(post("/api/v1/chat/threads"))
                .andExpect(status().isCreated());

        verify(chatService).createThread("general");
    }

    @Test
    void openThreads_WithCategoryParam_PassesCategoryToService() throws Exception {
        when(chatService.openThreads("order_issue")).thenReturn(List.of(thread("open", "order_issue")));

        mockMvc.perform(get("/api/v1/chat/threads?category=order_issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("order_issue"));

        verify(chatService).openThreads("order_issue");
    }

    @Test
    void closeThread_Returns200WithClosedStatus() throws Exception {
        when(chatService.closeThread(5)).thenReturn(
                new ChatThreadDto(5, UUID.randomUUID(), "Bob", "bob",
                        "bob@test.com", null, UUID.randomUUID(), "Agent Bob", "agentbob", "closed", "general",
                        OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(post("/api/v1/chat/threads/5/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("closed"));

        verify(chatService).closeThread(5);
    }
}
