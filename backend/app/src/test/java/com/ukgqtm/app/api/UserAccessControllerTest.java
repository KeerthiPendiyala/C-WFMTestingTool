package com.ukgqtm.app.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukgqtm.app.security.ApplicationUserAuthenticationToken;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.app.user.UserAccessApplicationService;
import com.ukgqtm.app.user.UserAccessApplicationService.UpdateUserCommand;
import com.ukgqtm.app.user.UserAccessApplicationService.UserStatus;
import com.ukgqtm.app.user.UserAccessApplicationService.UserSummary;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.AccessPermission;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserAccessController.class)
@Import(UserAccessControllerTest.TestSecurityConfig.class)
class UserAccessControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserAccessApplicationService users;

    @MockBean
    private AuthorizationPolicyService authorization;

    @Test
    void administratorCanUpdateUserAccess() throws Exception {
        AuthenticatedUser actor = user(true);
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UpdateUserCommand command = new UpdateUserCommand(
                "Mina",
                "Manager",
                "mina.manager@example.test",
                roleId,
                UserStatus.ACTIVE,
                List.of(projectId),
                "Updated1!Password",
                "Updated1!Password");
        when(users.updateUser(eq(actor), eq(userId), any(), any()))
                .thenReturn(new UserSummary(
                        userId,
                        "Mina",
                        "Manager",
                        "mina.manager@example.test",
                        roleId,
                        "Test Manager",
                        false,
                        UserStatus.ACTIVE.name(),
                        List.of(projectId),
                        List.of(AccessPermission.VIEW, AccessPermission.EDIT)));

        mockMvc.perform(patch("/api/v1/users/{userId}", userId)
                        .with(authentication(token(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Mina"))
                .andExpect(jsonPath("$.roleName").value("Test Manager"))
                .andExpect(jsonPath("$.projectIds[0]").value(projectId.toString()))
                .andExpect(jsonPath("$.permissions[1]").value("EDIT"));

        verifyNoInteractions(authorization);
    }

    @Test
    void nonAdministratorUpdateReturnsForbiddenBeforeUserLookup() throws Exception {
        AuthenticatedUser actor = user(false);
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UpdateUserCommand command = new UpdateUserCommand(
                "Alex",
                "Analyst",
                "alex.analyst@example.test",
                roleId,
                UserStatus.ACTIVE,
                List.of(UUID.randomUUID()),
                "",
                "");
        doThrow(new AccessDeniedException("The requested resource is not available."))
                .when(authorization)
                .require(
                        any(),
                        eq(AuthorizationPolicy.USER_ACCESS_MANAGE),
                        eq(null),
                        eq("APPLICATION_USER"),
                        eq("access-management"),
                        any());

        mockMvc.perform(patch("/api/v1/users/{userId}", userId)
                        .with(authentication(token(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The requested resource is not available."));

        verifyNoInteractions(users);
    }

    private static ApplicationUserAuthenticationToken token(AuthenticatedUser user) {
        return new ApplicationUserAuthenticationToken(user, null, List.of());
    }

    private static AuthenticatedUser user(boolean administrator) {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                UUID.randomUUID().toString(),
                "Avery",
                "Tester",
                "avery@example.test",
                administrator);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable);
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
            http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(401);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401}");
            }));
            return http.build();
        }
    }
}
