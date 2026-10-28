package com.willembergfilho.ifinance.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GitHubOAuth2UserService gitHubUserService;
    private final SyncingOidcUserService oidcUserService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("#{'${app.cors.allowed-origins}'.split(',')}")
    private List<String> corsAllowedOrigins;

    public SecurityConfig(GitHubOAuth2UserService gitHubUserService, SyncingOidcUserService oidcUserService) {
        this.gitHubUserService = gitHubUserService;
        this.oidcUserService = oidcUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // XHR calls get a 401 instead of a redirect to the provider (which the browser can't follow cross-origin);
            // browser navigations outside /api keep the OAuth2 login redirect.
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), pathPattern("/api/**")))
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(gitHubUserService)
                    .oidcUserService(oidcUserService))
                .successHandler((request, response, authentication) ->
                    response.sendRedirect(frontendBaseUrl + "/auth/callback"))
                .failureHandler((request, response, exception) -> {
                    String code = exception instanceof OAuth2AuthenticationException oauth2
                            ? oauth2.getError().getErrorCode() : "login_failed";
                    response.sendRedirect(frontendBaseUrl + "/?loginError="
                            + URLEncoder.encode(code, StandardCharsets.UTF_8));
                })
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
