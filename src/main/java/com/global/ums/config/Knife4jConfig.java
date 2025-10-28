package com.global.ums.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * Knife4j (Swagger) 接口文档配置
 */
@Configuration
@EnableSwagger2
public class Knife4jConfig {

    /**
     * 创建API文档
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                // API基本信息
                .apiInfo(apiInfo())
                // 是否启用
                .enable(true)
                .select()
                // 扫描的包路径
                .apis(RequestHandlerSelectors.basePackage("com.global.ums.controller"))
                // 扫描所有路径
                .paths(PathSelectors.any())
                .build()
                // 设置安全模式（JWT认证）
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    /**
     * API基本信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("UMS用户管理系统 API文档")
                .description("用户管理系统接口文档，包含用户管理、认证授权、微信集成等功能")
                .termsOfServiceUrl("https://www.example.com")
                .contact(new Contact("UMS Team", "https://www.example.com", "contact@example.com"))
                .version("1.0.0")
                .build();
    }

    /**
     * 安全模式配置（JWT）
     */
    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> apiKeyList = new ArrayList<>();
        apiKeyList.add(new ApiKey("Authorization", "Authorization", "header"));
        return apiKeyList;
    }

    /**
     * 安全上下文配置
     */
    private List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(
                SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        // 所有接口都需要认证（除了被排除的）
                        .forPaths(PathSelectors.regex("^(?!auth).*$"))
                        .build()
        );
        return securityContexts;
    }

    /**
     * 默认的安全引用
     */
    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> securityReferences = new ArrayList<>();
        securityReferences.add(new SecurityReference("Authorization", authorizationScopes));
        return securityReferences;
    }
}
