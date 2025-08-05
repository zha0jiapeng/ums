package com.global.ums.controller.system;

import com.global.ums.annotation.RequireAuth;
import com.global.ums.properties.AppVersionProperties;
import com.global.ums.result.AjaxResult;
import com.global.ums.utils.KeyValidationUtils;
import com.global.ums.utils.MessageUtils;
import com.global.ums.utils.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置管理控制器
 */
@RestController
@RequestMapping("/system")
@RequireAuth
public class ConfigController {

    @Autowired
    private AppVersionProperties appVersionProperties;

    /**
     * 获取版本信息（无需认证，启用Brotli压缩）
     */
    @RequireAuth(required = false)
    @GetMapping("/version")
    public AjaxResult version() {
        try {
            Map<String, Object> versionInfo = new HashMap<>();
            
            // 从配置文件获取应用版本信息
            versionInfo.put("appName", appVersionProperties.getName());
            versionInfo.put("version", appVersionProperties.getVersion());
            versionInfo.put("buildTime", appVersionProperties.getBuildTime());
           // versionInfo.put("author", appVersionProperties.getAuthor());
            versionInfo.put("description", SpringUtils.getBean(MessageUtils.class).getMessage(appVersionProperties.getDescriptionKey()));
            
            // 获取Java和系统信息
            versionInfo.put("javaVersion", System.getProperty("java.version"));
            versionInfo.put("osName", System.getProperty("os.name"));
            versionInfo.put("serverTime", java.time.LocalDateTime.now().toString());
            
            return AjaxResult.success(versionInfo);
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("system.version.error", e.getMessage()));
        }
    }

    /**
     * 手动重新加载key配置
     */
    @PostMapping("/reload-keys")
    public AjaxResult reloadKeyConfig() {
        try {
            KeyValidationUtils.reloadConfig();
            return AjaxResult.successI18n("config.reload.success");
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("config.reload.error", e.getMessage()));
        }
    }
    
    /**
     * 获取当前key配置信息
     */
    @GetMapping("/keys")
    public AjaxResult getKeyConfig() {
        try {
            Map<String, Object> result = new HashMap<>();
            Map<String, KeyValidationUtils.KeyConfig> allowedKeys = KeyValidationUtils.getAllowedKeys();
            
            // 为返回的key配置添加国际化描述
            Map<String, Object> i18nKeys = new HashMap<>();
            for (Map.Entry<String, KeyValidationUtils.KeyConfig> entry : allowedKeys.entrySet()) {
                String keyName = entry.getKey();
                KeyValidationUtils.KeyConfig config = entry.getValue();
                
                Map<String, Object> configInfo = new HashMap<>();
                configInfo.put("scope", config.getScope());
                configInfo.put("description", config.getDescription()); // 原始描述（向后兼容）
                configInfo.put("i18nDescription", config.getI18nDescription()); // 国际化描述
                configInfo.put("maxSize", config.getMaxSize());
                
                i18nKeys.put(keyName, configInfo);
            }
            
            result.put("allowedKeys", i18nKeys);
            result.put("totalCount", allowedKeys.size());
            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("config.get.error", e.getMessage()));
        }
    }
    
    /**
     * 检查指定key是否被允许
     */
    @GetMapping("/keys/check")
    public AjaxResult checkKey(@RequestParam String key) {
        try {
            boolean allowed = KeyValidationUtils.isKeyAllowed(key);
            Map<String, Object> result = new HashMap<>();
            result.put("key", key);
            result.put("allowed", allowed);
            
            if (allowed) {
                KeyValidationUtils.KeyConfig config = KeyValidationUtils.getKeyConfig(key);
                
                // 构造包含国际化信息的配置对象
                Map<String, Object> configInfo = new HashMap<>();
                configInfo.put("scope", config.getScope());
                configInfo.put("description", config.getDescription()); // 原始描述
                configInfo.put("i18nDescription", config.getI18nDescription()); // 国际化描述
                configInfo.put("maxSize", config.getMaxSize());
                
                result.put("config", configInfo);
            }
            
            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("config.key.check.error", e.getMessage()));
        }
    }
} 