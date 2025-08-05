package com.global.ums.factory;

import com.global.ums.interceptor.JwtAuthInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * JwtAuthInterceptor工厂类，用于打破循环依赖
 */
@Component
public class JwtAuthInterceptorFactory implements ObjectFactory<JwtAuthInterceptor>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public JwtAuthInterceptor getObject() throws BeansException {
        return applicationContext.getBean(JwtAuthInterceptor.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
} 