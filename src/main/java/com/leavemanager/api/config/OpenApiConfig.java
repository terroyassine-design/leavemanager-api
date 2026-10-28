package com.leavemanager.api.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** Swagger UI available at /swagger-ui.html, with an "Authorize" button for the JWT. */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "LeaveManager API", version = "v1",
                description = "Gestion des demandes de congés : les employés posent leurs congés, les managers les valident."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
