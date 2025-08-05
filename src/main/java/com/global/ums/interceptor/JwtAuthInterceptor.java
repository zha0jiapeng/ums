package com.global.ums.interceptor;

import com.global.ums.annotation.RequireAuth;
import com.global.ums.utils.JwtUtils;
import com.global.ums.utils.LoginUserContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * JWT认证拦截器
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.token-header:Authorization}")
    private String tokenHeader;

    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 获取方法上的RequireAuth注解
        RequireAuth methodAuth = handlerMethod.getMethodAnnotation(RequireAuth.class);
        // 获取类上的RequireAuth注解
        RequireAuth classAuth = handlerMethod.getBeanType().getAnnotation(RequireAuth.class);

        // 判断是否需要认证
        boolean requireAuth = false;
        if (methodAuth != null) {
            requireAuth = methodAuth.required();
        } else if (classAuth != null) {
            requireAuth = classAuth.required();
        }

        if (!requireAuth) {
             return true;
        }

        // 从请求头中获取token
        String authHeader = request.getHeader(tokenHeader);

        // 如果token存在，尝试验证token
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(tokenPrefix)) {
            String token = authHeader.substring(tokenPrefix.length());
            if (jwtUtils.validateToken(token) && jwtUtils.isAccessToken(token)) {
                // 验证通过，将用户信息存入上下文
                Map<String, Object> claims = jwtUtils.getAllClaimsFromToken(token);
                LoginUserContextHolder.setContext(claims);
                return true;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求处理完毕后，清除上下文，防止内存泄漏
        LoginUserContextHolder.clearContext();
        //log.info("======> JwtAuthInterceptor END: Cleared ThreadLocal context for URI = {}.", request.getRequestURI());
    }
} 