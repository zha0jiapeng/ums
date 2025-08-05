package com.global.ums.utils;

import java.util.Map;

/**
 * 登录用户上下文持有者
 * <p>
 * 使用ThreadLocal存储当前登录用户的信息，确保线程安全
 */
public class LoginUserContextHolder {

    private static final ThreadLocal<Map<String, Object>> userContext = new ThreadLocal<>();

    // 用户ID的键名
    public static final String KEY_USER_ID = "userId";
    // 用户类型的键名
    public static final String KEY_USER_TYPE = "userType";
    // 用户名的键名
    public static final String KEY_USERNAME = "username";

    /**
     * 设置用户信息到上下文
     *
     * @param claims 从JWT解析出的claims
     */
    public static void setContext(Map<String, Object> claims) {
        userContext.set(claims);
    }

    /**
     * 从上下文中获取所有用户信息
     *
     * @return 包含用户信息的Map
     */
    public static Map<String, Object> getContext() {
        return userContext.get();
    }

    /**
     * 从上下文中获取用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        Map<String, Object> context = getContext();
        if (context == null) {
            return null;
        }
        return (Long) context.get(KEY_USER_ID);
    }

    /**
     * 从上下文中获取用户类型
     *
     * @return 用户类型
     */
    public static Integer getUserType() {
        Map<String, Object> context = getContext();
        if (context == null) {
            return null;
        }
        return (Integer) context.get(KEY_USER_TYPE);
    }

    /**
     * 从上下文中获取用户名
     *
     * @return 用户名
     */
    public static String getUsername() {
        Map<String, Object> context = getContext();
        if (context == null) {
            return null;
        }
        return (String) context.get(KEY_USERNAME);
    }

    /**
     * 清除上下文
     */
    public static void clearContext() {
        userContext.remove();
    }
}
