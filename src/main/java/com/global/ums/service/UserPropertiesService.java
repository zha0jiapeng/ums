package com.global.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.dto.PropertyTreeDTO;
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
    List<UserProperties> getByUserId(Long userId,Boolean isHidden);
    
    /**
     * 根据用户ID和属性键获取属性
     *
     * @param userId 用户ID
     * @param key 属性键
     * @return 属性对象
     */
    UserProperties getByUserIdAndKey(Long userId, String key);

    /**
     * 根据用户ID和属性键获取所有属性（包括当前用户和所有父级用户的）
     *
     * @param userId 用户ID
     * @param key 属性键
     * @return 属性对象列表（第一个是当前用户的，后续是父级的）
     */
    List<UserProperties> getAllByUserIdAndKey(Long userId, String key);

    /**
     * 批量根据用户ID和属性键列表获取属性
     *
     * @param userId 用户ID
     * @param keys 属性键列表
     * @return 属性对象列表
     */
    List<UserProperties> batchGetByUserIdAndKeys(Long userId, List<String> keys);

    UserProperties getKeyisExist(String key,byte[] value);

    AjaxResult saveUserProperties(UserProperties userProperties);

    boolean saveUserPropertiesMap(Long id, Map<String,byte[]> map);

    /**
     * 填充属性的配置信息（从 ums_property_keys 表查询）
     * 包括: dataType, hidden, scope, description
     */
    void fillPropertyKeysInfo(UserProperties property);

    /**
     * 填充用户类型信息（从 ums_user 表查询）
     */
    void fillUserType(UserProperties property);

    /**
     * 根据模板同步关联用户属性
     *
     * @param templateId 模板ID
     * @param propertyDefaults 新增时的默认值集合
     * @param deleteKeys 删除时的属性键集合
     */
    void syncTemplateProperties(Long templateId,
                                List<Map<String, Object>> propertyDefaults,
                                List<String> deleteKeys);
} 
