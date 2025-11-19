package com.global.ums.config;

import com.global.ums.service.PropertyKeysService;
import com.global.ums.utils.KeyValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 属性键配置定时刷新任务
 * 自动从数据库同步最新的配置，无需手动调用刷新接口
 */
@Slf4j
@Component
public class PropertyKeysRefreshTask {

    @Autowired
    private PropertyKeysService propertyKeysService;

    /**
     * 定时刷新属性键配置缓存
     * 默认每30秒执行一次，可通过配置文件调整
     * 配置项: property.keys.refresh.interval (单位：毫秒)
     */
    @Scheduled(fixedDelayString = "${property.keys.refresh.interval:30000}")
    public void refreshPropertyKeys() {
        try {
            log.debug("开始自动刷新属性键配置缓存...");

            // 刷新 Service 层缓存
            propertyKeysService.refreshCache();

            // 刷新 KeyValidationUtils 缓存
            KeyValidationUtils.reloadConfig();

            log.debug("属性键配置缓存自动刷新完成");
        } catch (Exception e) {
            log.error("自动刷新属性键配置缓存失败", e);
        }
    }
}
