package com.global.ums.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class KeyValidationUtils {
    
    private static Map<String, KeyConfig> allowedKeys = new HashMap<>();
    private static volatile long lastModified = 0;
    
    private WatchService watchService;
    private ScheduledExecutorService executorService;
    private Path configPath;
    
    @PostConstruct
    public void init() {
        loadKeyConfig();
        startFileWatcher();
        startPeriodicCheck();
    }
    
    @PreDestroy
    public void destroy() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("关闭文件监听器失败", e);
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    /**
     * 启动文件监听器
     */
    private void startFileWatcher() {
        try {
            // 尝试获取配置文件的实际路径
            String configFilePath = getConfigFilePath();
            if (configFilePath != null) {
                configPath = Paths.get(configFilePath);
                Path configDir = configPath.getParent();
                
                watchService = FileSystems.getDefault().newWatchService();
                configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                
                // 启动监听线程
                Thread watchThread = new Thread(this::watchForChanges);
                watchThread.setDaemon(true);
                watchThread.setName("KeyValidation-FileWatcher");
                watchThread.start();
                
                log.info("启动文件监听器，监听配置文件: {}", configPath);
            }
        } catch (Exception e) {
            log.warn("启动文件监听器失败，将使用定时检查: {}", e.getMessage());
        }
    }
    
    /**
     * 启动定时检查（作为文件监听的备用方案）
     */
    private void startPeriodicCheck() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(this::checkAndReloadConfig, 30, 30, TimeUnit.SECONDS);
        log.info("启动定时检查，每30秒检查一次配置文件变更");
    }
    
    /**
     * 获取配置文件的实际路径
     */
    private String getConfigFilePath() {
        try {
            // 1. 优先监听外部配置文件
            java.io.File externalFile = new java.io.File("/app/config/key-validation-config.json");
            if (externalFile.exists()) {
                return externalFile.getAbsolutePath();
            }
            
            // 2. 回退到classpath文件
            ClassPathResource resource = new ClassPathResource("key-validation-config.json");
            if (resource.exists()) {
                return resource.getFile().getAbsolutePath();
            }
        } catch (Exception e) {
            log.debug("无法获取配置文件实际路径: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 文件监听循环
     */
    private void watchForChanges() {
        while (true) {
            try {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    Path fileName = (Path) event.context();
                    if ("key-validation-config.json".equals(fileName.toString())) {
                        log.info("检测到配置文件变更，重新加载配置");
                        // 延迟一下，确保文件写入完成
                        Thread.sleep(1000);
                        reloadConfig();
                    }
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("文件监听异常", e);
            }
        }
    }
    
    /**
     * 检查并重新加载配置（定时任务使用）
     */
    private void checkAndReloadConfig() {
        try {
            if (configPath != null && Files.exists(configPath)) {
                long currentModified = Files.getLastModifiedTime(configPath).toMillis();
                if (currentModified > lastModified) {
                    log.info("检测到配置文件时间戳变更，重新加载配置");
                    reloadConfig();
                }
            }
        } catch (Exception e) {
            log.error("检查配置文件变更失败", e);
        }
    }
    
    /**
     * 手动重新加载配置
     */
    public static void reloadConfig() {
        synchronized (KeyValidationUtils.class) {
            try {
                InputStream inputStream = null;
                String configSource = "";
                
                // 1. 首先尝试从外部配置路径读取（用于Docker热更新）
                try {
                    java.io.File externalFile = new java.io.File("/app/config/key-validation-config.json");
                    if (externalFile.exists()) {
                        inputStream = new java.io.FileInputStream(externalFile);
                        configSource = "外部文件: " + externalFile.getAbsolutePath();
                        log.info("从外部配置文件读取: {}", configSource);
                    }
                } catch (Exception e) {
                    log.debug("无法读取外部配置文件: {}", e.getMessage());
                }
                
                // 2. 如果外部文件不存在，从classpath读取
                if (inputStream == null) {
                    ClassPathResource resource = new ClassPathResource("key-validation-config.json");
                    if (resource.exists()) {
                        inputStream = resource.getInputStream();
                        configSource = "classpath: key-validation-config.json";
                        log.info("从classpath读取配置文件");
                    } else {
                        log.warn("Key配置文件 key-validation-config.json 不存在");
                        return;
                    }
                }
                
                Map<String, KeyConfig> newAllowedKeys = new HashMap<>();
                
                try {
                    StringBuilder jsonContent = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        jsonContent.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                    }
                    
                    JSONArray configArray = JSON.parseArray(jsonContent.toString());
                    
                    if (configArray != null) {
                        for (int i = 0; i < configArray.size(); i++) {
                            JSONObject keyConfigJson = configArray.getJSONObject(i);
                            String key = keyConfigJson.getString("key");
                            if (key == null || key.isEmpty()) {
                                log.warn("配置文件中存在无效的key配置，跳过: {}", keyConfigJson);
                                continue;
                            }
                            KeyConfig keyConfig = new KeyConfig();
                            keyConfig.setKey(key); // 设置key用于国际化
                            keyConfig.setScope(keyConfigJson.getInteger("scope"));
                            keyConfig.setDescription(keyConfigJson.getString("description"));
                            keyConfig.setMaxSize(keyConfigJson.getLong("maxSize"));
                            
                            newAllowedKeys.put(key, keyConfig);
                        }
                    }
                    
                    // 原子性更新配置
                    allowedKeys = newAllowedKeys;
                    lastModified = System.currentTimeMillis();
                    
                    log.info("🔄 成功重新加载 {} 个允许的key配置 (来源: {})", allowedKeys.size(), configSource);
                    
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            log.warn("关闭配置文件流失败: {}", e.getMessage());
                        }
                    }
                }
                
            } catch (IOException e) {
                log.error("重新加载Key配置文件失败: {}", e.getMessage());
            } catch (Exception e) {
                log.error("重新解析Key配置文件失败: {}", e.getMessage());
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
     */
    public static boolean isKeyAllowed(String key) {
        return allowedKeys.containsKey(key);
    }
    
    /**
     * 获取key的配置
     */
    public static KeyConfig getKeyConfig(String key) {
        return allowedKeys.get(key);
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
        
        // getter/setter
        public Integer getScope() { return scope; }
        public void setScope(Integer scope) { this.scope = scope; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Long getMaxSize() { return maxSize; }
        public void setMaxSize(Long maxSize) { this.maxSize = maxSize; }
        
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
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