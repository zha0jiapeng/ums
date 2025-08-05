package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.entity.User;
import com.global.ums.entity.UserGroup;
import com.global.ums.entity.UserProperties;
import com.global.ums.mapper.UserPropertiesMapper;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UserGroupService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户属性服务实现类
 */
@Service
public class UserPropertiesServiceImpl extends ServiceImpl<UserPropertiesMapper, UserProperties> implements UserPropertiesService {

    private final ApplicationContext applicationContext;
    private UserService userService;
    private UserGroupService userGroupService;
    
    public UserPropertiesServiceImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    /**
     * 懒加载UserService，避免循环依赖
     */
    private UserService getUserService() {
        if (userService == null) {
            userService = applicationContext.getBean(UserService.class);
        }
        return userService;
    }
    
    /**
     * 懒加载UserGroupService，避免循环依赖
     */
    private UserGroupService getUserGroupService() {
        if (userGroupService == null) {
            userGroupService = applicationContext.getBean(UserGroupService.class);
        }
        return userGroupService;
    }

    @Override
    public List<UserProperties> getByUserId(Long userId) {
        LambdaQueryWrapper<UserProperties> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserProperties::getUserId, userId);
        return this.list(queryWrapper);
    }
    
    @Override
    public UserProperties getByUserIdAndKey(Long userId, String key) {
        LambdaQueryWrapper<UserProperties> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserProperties::getUserId, userId)
                .eq(UserProperties::getKey, key);
        
        // 首先查找当前用户的属性
        UserProperties userProperties = this.getOne(queryWrapper);
        
        // 收集所有父级用户的同key属性
        List<UserProperties> parentProperties = new ArrayList<>();
        List<UserGroup> userGroups = getUserGroupService().getByUserId(userId);   
        if (userGroups.size() != 0) {
            collectAllParentProperties(userGroups, key, parentProperties, new ArrayList<>());
        }
        
        // 如果当前用户没有该属性，但有父级属性，创建一个空的用户属性对象
        if (userProperties == null && !parentProperties.isEmpty()) {
            userProperties = new UserProperties();
            userProperties.setUserId(userId);
            userProperties.setKey(key);
            userProperties.setValue(null);
        }
        
        // 设置父级属性列表
        if (userProperties != null) {
            userProperties.setParentProperties(parentProperties);
        }
        
        return userProperties;
    }
    
    /**
     * 递归收集所有父级用户的指定key属性
     * 
     * @param userGroups 当前用户的用户组列表
     * @param key 要查找的属性key
     * @param parentProperties 收集父级属性的列表
     * @param visitedUsers 已访问的用户ID列表，防止循环引用
     */
    private void collectAllParentProperties(List<UserGroup> userGroups, String key, List<UserProperties> parentProperties, List<Long> visitedUsers) {
        for (UserGroup userGroup : userGroups) {
            Long parentUserId = userGroup.getParentUserId();
            if (parentUserId != null && !visitedUsers.contains(parentUserId)) {
                visitedUsers.add(parentUserId);
                
                // 查找父级用户的属性
                UserProperties parentProperty = getDirectUserProperty(parentUserId, key);
                if (parentProperty != null) {
                    parentProperties.add(parentProperty);
                }
                
                // 继续递归查找父级的父级
                List<UserGroup> parentGroups = getUserGroupService().getByUserId(parentUserId);
                if (parentGroups.size() != 0) {
                    collectAllParentProperties(parentGroups, key, parentProperties, new ArrayList<>(visitedUsers));
                }
            }
        }
    }
    
    /**
     * 直接查找指定用户的属性（不递归）
     * 
     * @param userId 用户ID
     * @param key 属性key
     * @return 用户属性
     */
    private UserProperties getDirectUserProperty(Long userId, String key) {
        LambdaQueryWrapper<UserProperties> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserProperties::getUserId, userId)
                .eq(UserProperties::getKey, key);
        return this.getOne(queryWrapper);
    }

    @Override
    public UserProperties getKeyisExist(String key, byte[] value) {
        LambdaQueryWrapper<UserProperties> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserProperties::getKey, key);
        List<UserProperties> list = list(queryWrapper);
        for (UserProperties userProperties : list) {
            if (java.util.Arrays.equals(userProperties.getValue(), value)) {
                return userProperties;
            }
        }
        return null;
    }

    @Override
    public AjaxResult saveUserProperties(UserProperties userProperties) {
        User user = getUserService().getById(userProperties.getUserId());
        if(user == null){
            return AjaxResult.errorI18n("user.not.exists");
        }
        UserProperties userPropertiesInDb = getOne(new LambdaQueryWrapper<UserProperties>()
                .eq(UserProperties::getUserId, userProperties.getUserId())
                .eq(UserProperties::getKey, userProperties.getKey()),
                false
        );
        if(userPropertiesInDb== null ){
            save(userProperties);
            return AjaxResult.successI18n("user.properties.add.success");
        }else{
            userPropertiesInDb.setValue(userProperties.getValue());
            updateById(userPropertiesInDb);
            return AjaxResult.successI18n("user.properties.update.success");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveUserPropertiesMap(Long userId, Map<String, byte[]> map) {
        List<UserProperties> list = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            UserProperties userProperties = getByUserIdAndKey(userId, entry.getKey());
            if(userProperties==null)
                userProperties = new UserProperties();
            String key = entry.getKey();
            byte[] value = entry.getValue();
            userProperties.setUserId(userId);
            userProperties.setKey(key);
            userProperties.setValue(value);
            userProperties.setScope(2);
            list.add(userProperties);
        }
        return saveOrUpdateBatch(list);
    }
} 