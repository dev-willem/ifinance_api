package com.willembergfilho.ifinance.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${spring.security.oauth2.client.provider.google.authorization-uri:https://accounts.google.com/o/oauth2/auth}")
    private String authorizationUri;

    @Value("${spring.security.oauth2.client.provider.google.token-uri:https://oauth2.googleapis.com/token}")
    private String tokenUri;

    @Bean
    public OpenAPI openAPI() {

        String securitySchemeName = "google-oauth";

        return new OpenAPI()
                .info(new Info()
                        .title("iFinance API")
                        .version("v1")
                        .description("API documentation"))

                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securitySchemeName)
                )

                .components(
                        new Components()
                                .addSecuritySchemes(
                                        securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.OAUTH2)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .flows(
                                                        new OAuthFlows()
                                                                .authorizationCode(
                                                                        new OAuthFlow()
                                                                                .authorizationUrl(authorizationUri)
                                                                                .tokenUrl(tokenUri)
                                                                                .scopes(new Scopes()
                                                                                        .addString("openid", "OpenID")
                                                                                        .addString("profile", "User profile")
                                                                                        .addString("email", "User email"))
                                                                )
                                                )
                                )
                );
    }
}