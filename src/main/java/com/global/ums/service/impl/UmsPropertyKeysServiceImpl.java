package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.entity.UmsPropertyKeys;
import com.global.ums.mapper.UmsPropertyKeysMapper;
import com.global.ums.service.UmsPropertyKeysService;
import lombok.extern.slf4j.Slf4j;
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
public class UmsPropertyKeysServiceImpl extends ServiceImpl<UmsPropertyKeysMapper, UmsPropertyKeys> implements UmsPropertyKeysService {

    /**
     * 缓存所有配置，key -> UmsPropertyKeys
     */
    private final Map<String, UmsPropertyKeys> keysCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Override
    public UmsPropertyKeys getByKey(String key) {
        // 先从缓存获取
        UmsPropertyKeys config = keysCache.get(key);
        if (config != null) {
            return config;
        }

        // 缓存未命中，从数据库查询
        LambdaQueryWrapper<UmsPropertyKeys> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsPropertyKeys::getKey, key);
        config = getOne(wrapper);

        // 更新缓存
        if (config != null) {
            keysCache.put(key, config);
        }

        return config;
    }

    @Override
    public Map<String, UmsPropertyKeys> getAllKeysMap() {
        return new HashMap<>(keysCache);
    }

    @Override
    public void refreshCache() {
        try {
            List<UmsPropertyKeys> allKeys = list();
            keysCache.clear();
            for (UmsPropertyKeys key : allKeys) {
                if (key.getKey() != null && !key.getKey().isEmpty()) {
                    keysCache.put(key.getKey(), key);
                }
            }
            log.info("成功刷新属性键配置缓存，共 {} 个配置", keysCache.size());
        } catch (Exception e) {
            log.error("刷新属性键配置缓存失败", e);
        }
    }
}
