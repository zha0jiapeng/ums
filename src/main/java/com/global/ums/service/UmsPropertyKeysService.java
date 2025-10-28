package com.global.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.entity.UmsPropertyKeys;

import java.util.List;
import java.util.Map;

/**
 * 属性键配置服务接口
 */
public interface UmsPropertyKeysService extends IService<UmsPropertyKeys> {

    /**
     * 根据key获取配置
     *
     * @param key 属性键名
     * @return 配置对象
     */
    UmsPropertyKeys getByKey(String key);

    /**
     * 获取所有配置的Map
     *
     * @return key -> 配置对象的Map
     */
    Map<String, UmsPropertyKeys> getAllKeysMap();

    /**
     * 刷新缓存
     */
    void refreshCache();

    /**
     * 检查该 key 是否在 UserProperties 表中使用过
     * @param key 属性键名
     * @return true-已使用, false-未使用
     */
    boolean isKeyUsedInUserProperties(String key);
}
