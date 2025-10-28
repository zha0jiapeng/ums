package com.global.ums.utils;

import com.global.ums.entity.UmsPropertyKeys;
import com.global.ums.service.UmsPropertyKeysService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class KeyValidationUtils {

    private static Map<String, KeyConfig> allowedKeys = new HashMap<>();

    private static UmsPropertyKeysService propertyKeysService;

    @Autowired
    public void setPropertyKeysService(UmsPropertyKeysService propertyKeysService) {
        KeyValidationUtils.propertyKeysService = propertyKeysService;
    }

    @PostConstruct
    public void init() {
        loadKeyConfig();
    }
    
    /**
     * 手动重新加载配置
     */
    public static void reloadConfig() {
        synchronized (KeyValidationUtils.class) {
            try {
                if (propertyKeysService == null) {
                    log.warn("UmsPropertyKeysService 未初始化，无法加载配置");
                    return;
                }

                // 从数据库加载配置
                Map<String, UmsPropertyKeys> keysMap = propertyKeysService.getAllKeysMap();
                Map<String, KeyConfig> newAllowedKeys = new HashMap<>();

                for (Map.Entry<String, UmsPropertyKeys> entry : keysMap.entrySet()) {
                    UmsPropertyKeys propertyKey = entry.getValue();
                    KeyConfig keyConfig = new KeyConfig();
                    keyConfig.setKey(propertyKey.getKey());
                    keyConfig.setScope(propertyKey.getScope());
                    keyConfig.setDescription(propertyKey.getDescription());
                    keyConfig.setMaxSize(propertyKey.getSize());
                    keyConfig.setHidden(propertyKey.getHidden());

                    newAllowedKeys.put(propertyKey.getKey(), keyConfig);
                }

                // 原子性更新配置
                allowedKeys = newAllowedKeys;

                log.info("成功从数据库加载 {} 个允许的key配置", allowedKeys.size());

            } catch (Exception e) {
                log.error("从数据库加载Key配置失败", e);
            }
        }
    }
    
    /**
     * 从JSON文件加载key配置
     */
    private void loadKeyConfig() {
        reloadConfig();
    }
    
    /**
     * 检查key是否被允许
     * 如果缓存中不存在，会尝试从数据库实时查询
     */
    public static boolean isKeyAllowed(String key) {
        if (allowedKeys.containsKey(key)) {
            return true;
        }

        // 缓存未命中，尝试从数据库实时查询
        if (propertyKeysService != null) {
            UmsPropertyKeys propertyKey = propertyKeysService.getByKey(key);
            if (propertyKey != null) {
                // 更新本地缓存
                KeyConfig keyConfig = new KeyConfig();
                keyConfig.setKey(propertyKey.getKey());
                keyConfig.setScope(propertyKey.getScope());
                keyConfig.setDescription(propertyKey.getDescription());
                keyConfig.setMaxSize(propertyKey.getSize());
                keyConfig.setHidden(propertyKey.getHidden());
                allowedKeys.put(propertyKey.getKey(), keyConfig);

                log.debug("从数据库加载新的key配置: {}", key);
                return true;
            }
        }

        return false;
    }
    
    /**
     * 获取key的配置
     * 如果缓存中不存在，会尝试从数据库实时查询
     */
    public static KeyConfig getKeyConfig(String key) {
        KeyConfig config = allowedKeys.get(key);

        // 缓存未命中，尝试从数据库实时查询
        if (config == null && propertyKeysService != null) {
            UmsPropertyKeys propertyKey = propertyKeysService.getByKey(key);
            if (propertyKey != null) {
                config = new KeyConfig();
                config.setKey(propertyKey.getKey());
                config.setScope(propertyKey.getScope());
                config.setDescription(propertyKey.getDescription());
                config.setMaxSize(propertyKey.getSize());
                config.setHidden(propertyKey.getHidden());
                allowedKeys.put(propertyKey.getKey(), config);

                log.debug("从数据库加载新的key配置: {}", key);
            }
        }

        return config;
    }
    
    /**
     * 获取所有允许的key
     */
    public static Map<String, KeyConfig> getAllowedKeys() {
        return new HashMap<>(allowedKeys);
    }
    
    /**
     * 验证key和文件大小
     */
    public static ValidationResult validateKey(String key, long fileSize) {
        if (!isKeyAllowed(key)) {
            try {
                MessageUtils messageUtils = SpringUtils.getBean(MessageUtils.class);
                String errorMessage = messageUtils.getMessage("key.validation.not.allowed", key);
                return new ValidationResult(false, errorMessage);
            } catch (Exception e) {
                return new ValidationResult(false, "不允许的key: " + key);
            }
        }
        
        KeyConfig config = getKeyConfig(key);
        if (config.getMaxSize() != null && fileSize > config.getMaxSize()) {
            try {
                MessageUtils messageUtils = SpringUtils.getBean(MessageUtils.class);
                String errorMessage = messageUtils.getMessage("key.validation.size.exceeded", config.getMaxSize());
                return new ValidationResult(false, errorMessage);
            } catch (Exception e) {
                return new ValidationResult(false, "文件大小超过限制，最大允许: " + config.getMaxSize() + " 字节");
            }
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Key配置类
     */
    public static class KeyConfig {
        private Integer scope;
        private String description;
        private Long maxSize;
        private String key; // 用于国际化
        private Integer hidden; // 是否隐藏

        // getter/setter
        public Integer getScope() { return scope; }
        public void setScope(Integer scope) { this.scope = scope; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Long getMaxSize() { return maxSize; }
        public void setMaxSize(Long maxSize) { this.maxSize = maxSize; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public Integer getHidden() { return hidden; }
        public void setHidden(Integer hidden) { this.hidden = hidden; }
        
        /**
         * 获取国际化描述
         * 根据当前语言环境返回对应的配置描述
         * 
         * @return 国际化的配置描述
         */
        public String getI18nDescription() {
            if (key == null || key.isEmpty()) {
                return description; // 回退到默认描述
            }
            
            try {
                MessageUtils messageUtils = SpringUtils.getBean(MessageUtils.class);
                String messageKey = "key.config." + key.replace("-", "-"); // 保持key格式一致
                return messageUtils.getMessage(messageKey, description); // 如果找不到国际化消息，使用默认描述
            } catch (Exception e) {
                // 如果获取国际化消息失败，返回默认描述
                return description;
            }
        }
        
        /**
         * 根据key获取国际化描述（静态方法）
         * 
         * @param keyName key名称
         * @return 国际化的配置描述
         */
        public static String getI18nDescriptionByKey(String keyName) {
            if (keyName == null || keyName.isEmpty()) {
                return "";
            }
            
            try {
                MessageUtils messageUtils = SpringUtils.getBean(MessageUtils.class);
                String messageKey = "key.config." + keyName;
                return messageUtils.getMessage(messageKey);
            } catch (Exception e) {
                return keyName; // 如果找不到，返回key本身
            }
        }
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
} 