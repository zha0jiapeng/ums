package com.global.ums.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Locale;

/**
 * 国际化配置
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * 语言解析器 - 基于 Accept-Language 请求头
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        // 设置默认语言为中文
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        // 设置支持的语言列表
        resolver.setSupportedLocales(Arrays.asList(
                Locale.SIMPLIFIED_CHINESE,  // zh_CN
                Locale.CHINESE,             // zh
                Locale.US,                  // en_US
                Locale.ENGLISH              // en
        ));
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptorAdapter() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String acceptLanguage = request.getHeader("Accept-Language");
                // 调试日志：确认请求头是否到达
                System.out.println("[I18N DEBUG] Accept-Language header: " + acceptLanguage);

                // 从 Accept-Language 请求头解析 Locale 并设置到 LocaleContextHolder
                Locale locale = resolveLocale(request);
                System.out.println("[I18N DEBUG] Resolved locale: " + locale);
                LocaleContextHolder.setLocale(locale);
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                // 清理 ThreadLocal
                LocaleContextHolder.resetLocaleContext();
            }

            private Locale resolveLocale(HttpServletRequest request) {
                // 优先使用自定义请求头 X-Lang（避免被网关过滤）
                String lang = request.getHeader("X-Lang");
                if (lang == null || lang.isEmpty()) {
                    // 其次使用标准 Accept-Language
                    lang = request.getHeader("Accept-Language");
                }
                if (lang == null || lang.isEmpty()) {
                    return Locale.SIMPLIFIED_CHINESE;
                }
                // 解析语言，取第一个
                lang = lang.split(",")[0].trim();
                // 处理 zh-CN, zh_CN, zh 等格式
                if (lang.toLowerCase().startsWith("zh")) {
                    return Locale.SIMPLIFIED_CHINESE;
                } else if (lang.toLowerCase().startsWith("en")) {
                    return Locale.US;
                }
                return Locale.SIMPLIFIED_CHINESE;
            }
        }).addPathPatterns("/**");
    }
}
