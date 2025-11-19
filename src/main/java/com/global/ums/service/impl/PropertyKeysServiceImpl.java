package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.entity.PropertyKeys;
import com.global.ums.entity.UserProperties;
import com.global.ums.mapper.PropertyKeysMapper;
import com.global.ums.service.PropertyKeysService;
import com.global.ums.service.UserPropertiesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性键配置服务实现类
 */
@Slf4j
@Service
public class PropertyKeysServiceImpl extends ServiceImpl<PropertyKeysMapper, PropertyKeys> implements PropertyKeysService {

    /**
     * 缓存所有配置，key -> PropertyKeys
     */
    private final Map<String, PropertyKeys> keysCache = new ConcurrentHashMap<>();

    @Autowired
    private UserPropertiesService userPropertiesService;

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Override
    public PropertyKeys getByKey(String key) {
        // 先从缓存获取
        PropertyKeys config = keysCache.get(key);
        if (config != null) {
            return config;
        }

        // 缓存未命中，从数据库查询
        LambdaQueryWrapper<PropertyKeys> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PropertyKeys::getKey, key);
        config = getOne(wrapper);

        // 更新缓存
        if (config != null) {
            keysCache.put(key, config);
        }

        return config;
    }

    @Override
    public Map<String, PropertyKeys> getAllKeysMap() {
        return new HashMap<>(keysCache);
    }

    @Override
    public void refreshCache() {
        try {
            List<PropertyKeys> allKeys = super.list();
            keysCache.clear();
            for (PropertyKeys key : allKeys) {
                if (key.getKey() != null && !key.getKey().isEmpty()) {
                    keysCache.put(key.getKey(), key);
                }
            }
            log.info("成功刷新属性键配置缓存，共 {} 个配置", keysCache.size());
        } catch (Exception e) {
            log.error("刷新属性键配置缓存失败", e);
        }
    }

    @Override
    public boolean isKeyUsedInUserProperties(String key) {
        LambdaQueryWrapper<UserProperties> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProperties::getKey, key);
        return userPropertiesService.count(wrapper) > 0;
    }
}
