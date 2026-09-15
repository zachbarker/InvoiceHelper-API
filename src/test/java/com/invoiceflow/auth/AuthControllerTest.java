package com.invoiceflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerCreatesUserOrgAndReturnsAccessTokenPlusRefreshCookie() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "zach@example.com", "password123", "Zach Barker", "Zach's Freelance Co"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("zach@example.com"))
                .andExpect(jsonPath("$.organizationRole").value("OWNER"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void registeringTheSameEmailTwiceIsRejected() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "dupe@example.com", "password123", "Dupe User", "Dupe Co"
        );
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "login@example.com", "correct-password", "Login Test", "Login Co"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = new LoginRequest("login@example.com", "wrong-password");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginThenRefreshRotatesTheRefreshToken() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "refresh@example.com", "password123", "Refresh Test", "Refresh Co"
        );
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        jakarta.servlet.http.Cookie originalCookie = registerResult.getResponse().getCookie("refresh_token");
        org.junit.jupiter.api.Assertions.assertNotNull(originalCookie);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(originalCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        jakarta.servlet.http.Cookie rotatedCookie = refreshResult.getResponse().getCookie("refresh_token");
        org.junit.jupiter.api.Assertions.assertNotNull(rotatedCookie);
        org.junit.jupiter.api.Assertions.assertNotEquals(originalCookie.getValue(), rotatedCookie.getValue());

        // The original (now-rotated-away) token must no longer work.
        mockMvc.perform(post("/api/auth/refresh").cookie(originalCookie))
                .andExpect(status().isUnauthorized());
    }
}
