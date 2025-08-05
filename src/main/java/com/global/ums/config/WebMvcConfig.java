package com.global.ums.config;

import com.global.ums.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Knife4j资源映射
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        
        // Swagger UI资源映射
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        
        // 解决js、css等静态资源无法访问的问题
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
                
        // 静态资源映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 添加跨域支持，Knife4j可能需要
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 定义不需要拦截的路径
        List<String> excludePath = new ArrayList<>();
        // 登录/注册/密码相关
        excludePath.add("/user/login");
        excludePath.add("/user/register");
        excludePath.add("/user/password/**");
        // 微信相关
        excludePath.add("/wechat/**");
        // swagger/knife4j
        excludePath.add("/doc.html");
        excludePath.add("/swagger-ui.html");
        excludePath.add("/swagger-resources/**");
        excludePath.add("/webjars/**");
        excludePath.add("/v2/api-docs");
        // 静态资源
        excludePath.add("/static/**");
        excludePath.add("/assets/**");
        
        // 添加JWT认证拦截器
        registry.addInterceptor(jwtAuthInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                // 排除以上定义的路径
                .excludePathPatterns(excludePath);
    }
} 