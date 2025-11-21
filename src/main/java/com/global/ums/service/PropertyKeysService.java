package com.global.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.dto.PropertyKeysVO;
import com.global.ums.entity.PropertyKeys;

import java.util.List;
import java.util.Map;

/**
 * 属性键配置服务接口
 */
public interface PropertyKeysService extends IService<PropertyKeys> {

    /**
     * 根据key获取配置
     *
     * @param key 属性键名
     * @return 配置对象
     */
    PropertyKeys getByKey(String key);

    /**
     * 获取所有配置的Map
     *
     * @return key -> 配置对象的Map
     */
    Map<String, PropertyKeys> getAllKeysMap();

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

    /**
     * 根据ID获取属性键配置（包含枚举项）
     *
     * @param id 属性键ID
     * @return 属性键配置VO（包含枚举项）
     */
    PropertyKeysVO getByIdWithItems(Long id);

    /**
     * 根据key获取属性键配置（包含枚举项）
     *
     * @param key 属性键名
     * @return 属性键配置VO（包含枚举项）
     */
    PropertyKeysVO getByKeyWithItems(String key);

    /**
     * 获取所有属性键配置（包含枚举项）
     *
     * @return 属性键配置VO列表（包含枚举项）
     */
    List<PropertyKeysVO> listWithItems();
}
