package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.BakeryDto;
import com.sait.peelin.service.JwtService;
import com.sait.peelin.service.BakeryService;
import com.sait.peelin.service.TokenDenylistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = BakeryController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ClientWebSecurityAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.sait\\.peelin\\.security\\..*")
)
@AutoConfigureMockMvc(addFilters = false) // Disabling security filters for simplicity in this basic test
class BakeryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BakeryService bakeryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenDenylistService tokenDenylistService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void list_ShouldReturnOk() throws Exception {
        BakeryDto dto = new BakeryDto(1, "Bakery 1", "123", "email", null, null, null, null, null);
        when(bakeryService.list(null)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/bakeries"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Bakery 1"));
    }

    @Test
    void get_ShouldReturnOk() throws Exception {
        BakeryDto dto = new BakeryDto(1, "Bakery 1", "123", "email", null, null, null, null, null);
        when(bakeryService.get(1)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/bakeries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
