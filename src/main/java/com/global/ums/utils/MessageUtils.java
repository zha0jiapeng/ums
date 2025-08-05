package com.global.ums.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Locale;

/**
 * 国际化消息工具类
 */
@Component
public class MessageUtils {

    @Resource
    private MessageSource messageSource;

    /**
     * 根据消息键和参数获取消息
     * 
     * @param code 消息键
     * @param args 参数
     * @return 消息内容
     */
    public String getMessage(String code, Object... args) {
        return getMessage(code, null, args);
    }

    /**
     * 根据消息键和参数获取消息，如果找不到则返回默认消息
     * 
     * @param code 消息键
     * @param defaultMessage 默认消息
     * @param args 参数
     * @return 消息内容
     */
    public String getMessage(String code, String defaultMessage, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

    /**
     * 根据消息键获取消息（无参数）
     * 
     * @param code 消息键
     * @return 消息内容
     */
    public String getMessage(String code) {
        return getMessage(code, null);
    }
} 