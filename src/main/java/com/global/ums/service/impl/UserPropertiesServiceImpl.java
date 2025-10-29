package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.dto.PropertyTreeDTO;
import com.global.ums.entity.UmsPropertyKeys;
import com.global.ums.entity.User;
import com.global.ums.entity.UserGroup;
import com.global.ums.entity.UserProperties;
import com.global.ums.mapper.UserPropertiesMapper;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UmsPropertyKeysService;
import com.global.ums.service.UserGroupService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.service.UserService;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户属性服务实现类
 */
@Service
public class UserPropertiesServiceImpl extends ServiceImpl<UserPropertiesMapper, UserProperties> implements UserPropertiesService {

    private final ApplicationContext applicationContext;
    private UserService userService;
    private UserGroupService userGroupService;
    private UmsPropertyKeysService propertyKeysService;

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

    /**
     * 懒加载UmsPropertyKeysService，避免循环依赖
     */
    private UmsPropertyKeysService getPropertyKeysService() {
        if (propertyKeysService == null) {
            propertyKeysService = applicationContext.getBean(UmsPropertyKeysService.class);
        }
        return propertyKeysService;
    }

    @Override
    public List<UserProperties> getByUserId(Long userId, Boolean isHidden) {
        LambdaQueryWrapper<UserProperties> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserProperties::getUserId, userId);
        // 注意：isHidden 参数保留但不使用，因为 hidden 字段已从数据库移除
        // hidden 信息现在从 ums_property_keys 表中动态获取
        List<UserProperties> properties = this.list(queryWrapper);

        // 如果需要按 hidden 过滤，在查询后过滤
        if (isHidden != null) {
            properties.forEach(this::fillPropertyKeysInfo);
            properties.removeIf(prop -> !isHidden.equals(prop.getHidden()));
        }

        return properties;
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
            // 注意：scope 字段已从数据库移除，现在从 ums_property_keys 表中动态获取
            list.add(userProperties);
        }
        return saveOrUpdateBatch(list);
    }

    @Override
    public void fillPropertyKeysInfo(UserProperties property) {
        if (property != null && property.getKey() != null) {
            UmsPropertyKeys propertyKey = getPropertyKeysService().getByKey(property.getKey());
            if (propertyKey != null) {
                property.setDataType(propertyKey.getDataType());
                property.setHidden(propertyKey.getHidden());
                property.setScope(propertyKey.getScope());
                property.setDescription(propertyKey.getDescription());
            }
        }
    }

    @Override
    public List<PropertyTreeDTO> getApplicationPropertiesTree(Long userId) {
        // 1. 获取所有category=application的用户ID集合
        List<Long> applicationUserIds = list(new LambdaQueryWrapper<UserProperties>()
                .eq(UserProperties::getKey, "category"))
                .stream()
                .filter(prop -> prop.getValue() != null && "application".equals(new String(prop.getValue())))
                .map(UserProperties::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        if (applicationUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 获取所有user_group中的用户关系
        List<UserGroup> allUserGroups = getUserGroupService().list();
        
        // 从user_group中的user_id集合（即有父级的用户）
        Set<Long> usersWithParent = allUserGroups.stream()
                .map(UserGroup::getUserId)
                .collect(Collectors.toSet());
        
        // 3. 找出application用户中真正的根（在user_group中没有parent_user_id的）
        List<Long> rootUserIds = applicationUserIds.stream()
                .filter(id -> !usersWithParent.contains(id))
                .collect(Collectors.toList());
        
        // 4. 为每个根用户构建树形结构
        List<PropertyTreeDTO> result = new ArrayList<>();
        Set<Long> processedUsers = new HashSet<>();
        
        for (Long rootUserId : rootUserIds) {
            PropertyTreeDTO rootNode = buildApplicationTree(rootUserId, applicationUserIds, processedUsers);
            if (rootNode != null) {
                result.add(rootNode);
            }
        }
        
        return result;
    }
    
    /**
     * 为单个根用户构建应用树（只包含用户节点，不包含属性）
     */
    private PropertyTreeDTO buildApplicationTree(Long userId, List<Long> applicationUserIds, Set<Long> processedUsers) {
        if (processedUsers.contains(userId)) {
            return null;
        }
        processedUsers.add(userId);
        
        User user = getUserService().getById(userId);
        String username = user != null ? user.getUniqueId() : "";
        
        // 创建用户节点
        PropertyTreeDTO userNode = new PropertyTreeDTO();
        userNode.setUserId(userId);
        userNode.setUsername(username);
        userNode.setChildren(new ArrayList<>());
        
        // 获取该用户的所有子用户
        List<UserGroup> childGroups = getUserGroupService().getByParentUserId(userId);
        
        // 递归添加满足条件的子用户
        for (UserGroup childGroup : childGroups) {
            Long childUserId = childGroup.getUserId();
            if (applicationUserIds.contains(childUserId)) {
                PropertyTreeDTO childNode = buildApplicationTree(childUserId, applicationUserIds, processedUsers);
                if (childNode != null) {
                    userNode.getChildren().add(childNode);
                }
            }
        }
        
        return userNode;
    }
    
    /**
     * 将UserProperties转换为PropertyTreeDTO
     */
    private PropertyTreeDTO convertToPropertyTreeDTO(UserProperties prop, Long userId, String username) {
        if (prop == null || prop.getKey() == null) {
            return null;
        }
        
        PropertyTreeDTO dto = new PropertyTreeDTO();
        dto.setUserId(userId);
        dto.setUsername(username);
        dto.setKey(prop.getKey());
        
        // 将byte数组转换为字符串
        if (prop.getValue() != null) {
            try {
                dto.setValue(new String(prop.getValue(), "UTF-8"));
            } catch (Exception e) {
                dto.setValue(new String(prop.getValue()));
            }
        }
        
        // 填充属性key的元数据
        fillPropertyKeysInfo(prop);
        dto.setDataType(prop.getDataType());
        dto.setDescription(prop.getDescription());
        
        return dto;
    }
}
