package com.global.ums.annotation;

import java.lang.annotation.*;

/**
 * 需要认证注解
 * 用于标记需要进行认证的方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAuth {

    /**
     * 是否需要认证
     */
    boolean required() default true;
} 