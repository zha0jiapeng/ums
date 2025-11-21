package com.global.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.entity.PropertyKeyItems;

import java.util.List;

/**
 * 属性键枚举项服务接口
 */
public interface PropertyKeyItemsService extends IService<PropertyKeyItems> {

    /**
     * 根据属性键获取所有枚举项（按优先级排序）
     *
     * @param key 属性键名
     * @return 枚举项列表
     */
    List<PropertyKeyItems> getItemsByKey(String key);


    /**
     * 检查枚举值是否存在
     *
     * @param key 属性键名
     * @param itemValue 枚举值
     * @return true-存在, false-不存在
     */
    boolean existsItem(String key, String itemValue);
}
