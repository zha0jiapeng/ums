package com.global.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.entity.UserProperties;
import com.global.ums.result.AjaxResult;

import java.util.List;
import java.util.Map;

/**
 * 用户属性服务接口
 */
public interface UserPropertiesService extends IService<UserProperties> {
    
    /**
     * 根据用户ID获取属性列表
     * 
     * @param userId 用户ID
     * @return 属性列表
     */
    List<UserProperties> getByUserId(Long userId);
    
    /**
     * 根据用户ID和属性键获取属性
     * 
     * @param userId 用户ID
     * @param key 属性键
     * @return 属性对象
     */
    UserProperties getByUserIdAndKey(Long userId, String key);

    UserProperties getKeyisExist(String key,byte[] value);

    AjaxResult saveUserProperties(UserProperties userProperties);

    boolean saveUserPropertiesMap(Long id, Map<String,byte[]> map);
} 