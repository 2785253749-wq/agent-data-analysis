package com.agent;

import com.agent.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UserController")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepo;
    private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

    @BeforeEach
    void clean() {
        // Keep seed admin + analyst; delete users created by this test run.
        userRepo.findAll().stream()
                .filter(u -> !"admin".equals(u.getUsername()) && !"analyst".equals(u.getUsername()))
                .forEach(u -> userRepo.delete(u));
    }

    @Test
    @DisplayName("admin lists users without passwords")
    void listUsersNoPassword() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(httpBasic("admin", "test123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'admin')]").exists())
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("admin creates user with BCrypt-hashed password")
    void createUserHashesPassword() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .with(httpBasic("admin", "test123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "newanalyst", "password", "plain123",
                                "displayName", "新分析员", "role", "ANALYST"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newanalyst"))
                .andExpect(jsonPath("$.password").doesNotExist());

        var u = userRepo.findByUsername("newanalyst").orElseThrow();
        assertTrue(enc.matches("plain123", u.getPasswordHash()), "stored hash is BCrypt");
        assertFalse(u.getPasswordHash().contains("plain123"), "plaintext not stored");
    }

    @Test
    @DisplayName("analyst cannot access user management")
    void analystForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(httpBasic("analyst", "test123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("cannot disable the last enabled admin")
    void cannotDisableLastAdmin() throws Exception {
        // Only admin exists (seeded) → disabling self/last admin must fail.
        Long adminId = userRepo.findByUsername("admin").orElseThrow().getId();
        mockMvc.perform(put("/api/admin/users/{id}", adminId)
                        .with(httpBasic("admin", "test123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isEnabled", false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("reset password stores new BCrypt hash")
    void resetPassword() throws Exception {
        Long adminId = userRepo.findByUsername("admin").orElseThrow().getId();
        mockMvc.perform(post("/api/admin/users/{id}/reset-password", adminId)
                        .param("newPassword", "newpass456")
                        .with(httpBasic("admin", "test123")))
                .andExpect(status().isNoContent());

        var admin = userRepo.findByUsername("admin").orElseThrow();
        assertTrue(enc.matches("newpass456", admin.getPasswordHash()));
    }
}
