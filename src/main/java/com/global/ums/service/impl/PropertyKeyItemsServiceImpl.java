package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.entity.PropertyKeyItems;
import com.global.ums.mapper.PropertyKeyItemsMapper;
import com.global.ums.service.PropertyKeyItemsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 属性键枚举项服务实现类
 */
@Slf4j
@Service
public class PropertyKeyItemsServiceImpl extends ServiceImpl<PropertyKeyItemsMapper, PropertyKeyItems> implements PropertyKeyItemsService {

    @Override
    public List<PropertyKeyItems> getItemsByKey(String key) {
        LambdaQueryWrapper<PropertyKeyItems> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PropertyKeyItems::getKey, key)
                .orderByAsc(PropertyKeyItems::getPriority);
        return list(wrapper);
    }


    @Override
    public boolean existsItem(String key, String itemValue) {
        LambdaQueryWrapper<PropertyKeyItems> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PropertyKeyItems::getKey, key)
                .eq(PropertyKeyItems::getItemValue, itemValue);
        return count(wrapper) > 0;
    }
}
