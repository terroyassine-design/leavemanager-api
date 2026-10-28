package com.leavemanager.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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

/**
 * End-to-end test of the HTTP API: real Spring context, real security, in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaveWorkflowIntegrationTest {

    private static final String PASSWORD = "Password123";
    private static final LocalDate NEXT_MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

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
        userRepository.save(new User("Marie Manager", "manager@test.com", passwordEncoder.encode(PASSWORD), Role.MANAGER));
    }

    @Test
    void employeeRequestsLeaveAndManagerApprovesIt() throws Exception {
        String employeeToken = register("Jean Dupont", "jean@test.com");
        String managerToken = login("manager@test.com");

        String leaveJson = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", bearer(employeeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaveBody("PAID", NEXT_MONDAY, NEXT_MONDAY.plusDays(4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.workingDays").value(5))
                .andReturn().getResponse().getContentAsString();
        Integer leaveId = JsonPath.read(leaveJson, "$.id");

        mockMvc.perform(get("/api/manager/leaves?status=PENDING").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeName").value("Jean Dupont"));

        mockMvc.perform(patch("/api/manager/leaves/{id}/approve", leaveId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\": \"Bonnes vacances\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedByName").value("Marie Manager"));

        mockMvc.perform(get("/api/profile").header("Authorization", bearer(employeeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveBalance").value(User.DEFAULT_LEAVE_BALANCE - 5));
    }

    @Test
    void overlappingRequestsAreRejected() throws Exception {
        String token = register("Jean Dupont", "jean@test.com");

        mockMvc.perform(post("/api/leaves").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaveBody("PAID", NEXT_MONDAY, NEXT_MONDAY.plusDays(4))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/leaves").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaveBody("UNPAID", NEXT_MONDAY.plusDays(2), NEXT_MONDAY.plusDays(8))))
                .andExpect(status().isConflict());
    }

    @Test
    void employeeCannotUseManagerEndpoints() throws Exception {
        String token = register("Jean Dupont", "jean@test.com");

        mockMvc.perform(get("/api/manager/leaves").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestsWithoutTokenAreUnauthorized() throws Exception {
        mockMvc.perform(get("/api/leaves/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"manager@test.com\", \"password\": \"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"\", \"email\": \"not-an-email\", \"password\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
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

    private String login(String email) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD);
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private static String leaveBody(String type, LocalDate start, LocalDate end) {
        return """
                {"type": "%s", "startDate": "%s", "endDate": "%s", "reason": "Test"}
                """.formatted(type, start, end);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
