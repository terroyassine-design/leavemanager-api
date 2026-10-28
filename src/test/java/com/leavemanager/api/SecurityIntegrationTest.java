package com.leavemanager.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.leavemanager.api.auth.JwtService;
import com.leavemanager.api.leave.LeaveRequestRepository;
import com.leavemanager.api.user.Role;
import com.leavemanager.api.user.User;
import com.leavemanager.api.user.UserRepository;

/** Attacks the API the way an attacker would, and checks that each attempt is refused. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRequestRepository leaveRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    private User manager;

    @BeforeEach
    void setUp() {
        leaveRepository.deleteAll();
        userRepository.deleteAll();
        manager = userRepository.save(
                new User("Marie Manager", "manager@test.com", passwordEncoder.encode(PASSWORD), Role.MANAGER));
    }

    @Test
    void loginIsBlockedAfterTooManyFailures() throws Exception {
        // Dedicated account: the attempt counter is shared by every test using this Spring context
        userRepository.save(new User("Target", "target@test.com", passwordEncoder.encode(PASSWORD), Role.EMPLOYEE));
        for (int i = 0; i < 5; i++) {
            login("target@test.com", "wrong-password-" + i).andExpect(status().isUnauthorized());
        }

        // Even the right password is refused while the block lasts
        login("target@test.com", PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Trop de tentatives")));
    }

    @Test
    void successfulLoginResetsTheFailureCounter() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("manager@test.com", "wrong-password").andExpect(status().isUnauthorized());
        }
        login("manager@test.com", PASSWORD).andExpect(status().isOk());

        login("manager@test.com", "wrong-password").andExpect(status().isUnauthorized());
        login("manager@test.com", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() throws Exception {
        String forged = token("another-app", manager.getSessionVersion());

        mockMvc.perform(get("/api/manager/leaves").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutSessionVersionIsRejected() throws Exception {
        String forged = token(JwtService.ISSUER, null);

        mockMvc.perform(get("/api/manager/leaves").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenIsAccepted() throws Exception {
        String valid = token(JwtService.ISSUER, manager.getSessionVersion());

        mockMvc.perform(get("/api/manager/leaves").header("Authorization", "Bearer " + valid))
                .andExpect(status().isOk());
    }

    @Test
    void passwordLongerThan72BytesIsRejectedCleanly() throws Exception {
        // 40 characters pass @Size(max = 72), but "é" takes 2 bytes: 80 bytes > BCrypt's limit
        String body = """
                {"fullName": "Jean Dupont", "email": "jean@test.com", "password": "%s"}
                """.formatted("é".repeat(40));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("72 octets")));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Builds a token signed with the real key, to test the checks made after the signature. */
    private String token(String issuer, Integer sessionVersion) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(10, ChronoUnit.MINUTES))
                .subject(manager.getId().toString())
                .claim("roles", List.of("MANAGER"));
        if (sessionVersion != null) {
            claims.claim(JwtService.SESSION_VERSION_CLAIM, sessionVersion);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
