package com.leavemanager.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.leavemanager.api.leave.LeaveRequestRepository;
import com.leavemanager.api.user.Role;
import com.leavemanager.api.user.User;
import com.leavemanager.api.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRequestRepository leaveRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        leaveRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void returnsTheProfileOfTheLoggedInUser() throws Exception {
        String token = register("Jean Dupont", "jean@test.com");

        mockMvc.perform(get("/api/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jean Dupont"))
                .andExpect(jsonPath("$.email").value("jean@test.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.demo").value(false))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void updatesNameAndEmail() throws Exception {
        String token = register("Jean Dupont", "jean@test.com");

        mockMvc.perform(put("/api/profile").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Jean Martin\", \"email\": \"Jean.Martin@Test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jean Martin"))
                .andExpect(jsonPath("$.email").value("jean.martin@test.com"));

        // The new email works for login
        login("jean.martin@test.com", PASSWORD);
    }

    @Test
    void refusesAnEmailAlreadyUsedByAnotherAccount() throws Exception {
        register("Autre Personne", "autre@test.com");
        String token = register("Jean Dupont", "jean@test.com");

        mockMvc.perform(put("/api/profile").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Jean Dupont\", \"email\": \"autre@test.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void changingThePasswordRevokesOldTokensAndReturnsANewOne() throws Exception {
        String oldToken = register("Jean Dupont", "jean@test.com");

        String json = mockMvc.perform(put("/api/profile/password").header("Authorization", bearer(oldToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"%s\", \"newPassword\": \"NewPassword456\"}".formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newToken = JsonPath.read(json, "$.token");

        // A token stolen before the change is no longer accepted...
        mockMvc.perform(get("/api/profile").header("Authorization", bearer(oldToken)))
                .andExpect(status().isUnauthorized());
        // ...while the current session keeps working with the new token
        mockMvc.perform(get("/api/profile").header("Authorization", bearer(newToken)))
                .andExpect(status().isOk());

        login("jean@test.com", "NewPassword456");
    }

    @Test
    void refusesPasswordChangeWithAWrongCurrentPassword() throws Exception {
        String token = register("Jean Dupont", "jean@test.com");

        mockMvc.perform(put("/api/profile/password").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"wrong-password\", \"newPassword\": \"NewPassword456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Le mot de passe actuel est incorrect"));
    }

    @Test
    void demoAccountsCannotBeModified() throws Exception {
        User demo = new User("Demo User", "demo@demo.com", passwordEncoder.encode(PASSWORD), Role.EMPLOYEE);
        demo.markAsDemo();
        userRepository.save(demo);
        String token = login("demo@demo.com", PASSWORD);

        mockMvc.perform(put("/api/profile").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Hacked\", \"email\": \"demo@demo.com\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/profile/password").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"%s\", \"newPassword\": \"NewPassword456\"}".formatted(PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    private String register(String fullName, String email) throws Exception {
        String body = """
                {"fullName": "%s", "email": "%s", "password": "%s"}
                """.formatted(fullName, email, PASSWORD);
        String json = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String login(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
