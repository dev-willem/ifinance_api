package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.api.admin.AdminController;
import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.api.auth.AuthController;
import com.willembergfilho.ifinance.application.admin.UserAdministrationUseCase;
import com.willembergfilho.ifinance.infrastructure.persistence.InvestmentJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.SimulationJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.UserIdentityJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real SecurityConfig + controllers in a plain Spring MVC context with mocked repositories (no DB, no Boot). */
@SpringJUnitWebConfig(SecurityConfigTest.TestConfig.class)
@TestPropertySource(properties = {
        "app.frontend.base-url=http://front.test",
        "app.cors.allowed-origins=http://front.test",
        "app.security.admin-emails=admin@x.com"
})
class SecurityConfigTest {

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, GitHubOAuth2UserService.class, SyncingOidcUserService.class,
            UserSyncService.class, UserContextHolder.class, AuthController.class, AdminController.class,
            UserAdministrationUseCase.class, GlobalExceptionHandler.class})
    static class TestConfig {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(
                    CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId("id").clientSecret("s").build(),
                    CommonOAuth2Provider.GITHUB.getBuilder("github").clientId("id").clientSecret("s").build());
        }
    }

    @MockitoBean UserJpaRepository userRepository;
    @MockitoBean UserIdentityJpaRepository identityRepository;
    @MockitoBean SimulationJpaRepository simulationRepository;
    @MockitoBean InvestmentJpaRepository investmentRepository;

    @Autowired WebApplicationContext wac;

    private MockMvc mvc;
    private UserEntity admin;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        admin = new UserEntity(UUID.randomUUID(), "admin-sub", "admin@x.com", "Admin", "google", null, null);
        admin.setRole(Role.ADMIN);
        user = new UserEntity(UUID.randomUUID(), "github|9", "user@x.com", "User", "github", null, null);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Same principal shape our user services build: internal id attribute + ROLE_ authority. */
    private static RequestPostProcessor loginAs(UserEntity u) {
        return oauth2Login()
                .attributes(a -> {
                    a.put("sub", u.getSubjectId());
                    a.put(UserSyncService.USER_ID_ATTRIBUTE, u.getId().toString());
                })
                .authorities(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
    }

    // ---------------------------------------------------------- anonymous --

    @Test
    void anonymous_me_is401NotRedirect() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_admin_is401() throws Exception {
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_browserNavigationOutsideApi_stillRedirectsToLogin() throws Exception {
        mvc.perform(get("/swagger-ui.html").accept(MediaType.TEXT_HTML)).andExpect(status().is3xxRedirection());
    }

    @Test
    void anonymous_apiCallAcceptingHtml_isStill401() throws Exception {
        mvc.perform(get("/api/v1/users/me").accept(MediaType.TEXT_HTML)).andExpect(status().isUnauthorized());
    }

    @Test
    void githubAuthorizationEndpoint_redirectsToGitHub() throws Exception {
        mvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getRedirectedUrl())
                        .startsWith("https://github.com/login/oauth/authorize"));
    }

    // --------------------------------------------------------------- USER --

    @Test
    void user_me_returnsRoleAndProvider() throws Exception {
        mvc.perform(get("/api/v1/users/me").with(loginAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@x.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.provider").value("github"));
    }

    @Test
    void legacyPrincipalWithOnlySub_stillResolvesUser() throws Exception {
        when(userRepository.findBySubjectId("admin-sub")).thenReturn(Optional.of(admin));

        mvc.perform(get("/api/v1/users/me").with(oauth2Login().attributes(a -> a.put("sub", "admin-sub"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void user_adminEndpoints_are403() throws Exception {
        mvc.perform(get("/api/v1/admin/users").with(loginAs(user))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/stats").with(loginAs(user))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/users/{id}/role", user.getId()).with(loginAs(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------- ADMIN --

    @Test
    void admin_listsUsers() throws Exception {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(admin, user), PageRequest.of(0, 20), 2));

        mvc.perform(get("/api/v1/admin/users").with(loginAs(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.users[1].email").value("user@x.com"))
                .andExpect(jsonPath("$.users[1].provider").value("github"))
                .andExpect(jsonPath("$.users[1].role").value("USER"))
                .andExpect(jsonPath("$.users[0].bootstrapAdmin").value(true))
                .andExpect(jsonPath("$.users[1].bootstrapAdmin").value(false));
    }

    @Test
    void admin_invalidPageSize_is400() throws Exception {
        mvc.perform(get("/api/v1/admin/users").param("size", "500").with(loginAs(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_stats() throws Exception {
        when(userRepository.countGroupedByRole()).thenReturn(List.<Object[]>of(
                new Object[]{Role.ADMIN, 1L}, new Object[]{Role.USER, 1L}));
        when(userRepository.countGroupedByProvider()).thenReturn(List.<Object[]>of(
                new Object[]{"google", 1L}, new Object[]{"github", 1L}));
        when(simulationRepository.count()).thenReturn(5L);
        when(investmentRepository.count()).thenReturn(3L);

        mvc.perform(get("/api/v1/admin/stats").with(loginAs(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.usersByRole.ADMIN").value(1))
                .andExpect(jsonPath("$.usersByProvider.github").value(1))
                .andExpect(jsonPath("$.totalSimulations").value(5))
                .andExpect(jsonPath("$.totalInvestments").value(3));
    }

    @Test
    void admin_promotesUser() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/{id}/role", user.getId()).with(loginAs(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void admin_cannotDemoteSelf() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/{id}/role", admin.getId()).with(loginAs(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Você não pode remover seu próprio acesso de administrador."));
    }

    @Test
    void admin_unknownUser_is404() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/{id}/role", UUID.randomUUID()).with(loginAs(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void admin_invalidRole_is400() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/{id}/role", user.getId()).with(loginAs(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROOT\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/admin/users/{id}/role", user.getId()).with(loginAs(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void demotedAdminWithStaleSession_is403() throws Exception {
        UserEntity demoted = new UserEntity(UUID.randomUUID(), "d", "d@x.com", "D", "google", null, null);
        when(userRepository.findById(demoted.getId())).thenReturn(Optional.of(demoted));

        mvc.perform(get("/api/v1/admin/stats").with(oauth2Login()
                        .attributes(a -> a.put(UserSyncService.USER_ID_ATTRIBUTE, demoted.getId().toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }
}
