package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.constant.UserPropertiesConstant;
import com.global.ums.dto.PropertyTreeDTO;
import com.global.ums.entity.PropertyKeys;
import com.global.ums.entity.User;
import com.global.ums.entity.UserGroup;
import com.global.ums.entity.UserProperties;
import com.global.ums.mapper.UserPropertiesMapper;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.PropertyKeysService;
import com.global.ums.service.UserGroupService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.service.UserService;
import com.global.ums.utils.KeyValidationUtils;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户属性服务实现类
 */
@Service
public class UserPropertiesServiceImpl extends ServiceImpl<UserPropertiesMapper, UserProperties> implements UserPropertiesService {

    private final ApplicationContext applicationContext;
    private UserService userService;
    private UserGroupService userGroupService;
    private PropertyKeysService propertyKeysService;

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
     * 懒加载PropertyKeysService，避免循环依赖
     */
    private PropertyKeysService getPropertyKeysService() {
        if (propertyKeysService == null) {
            propertyKeysService = applicationContext.getBean(PropertyKeysService.class);
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
            properties.removeIf(prop -> {
                Integer hidden = prop.getHidden();
                if (isHidden) {
                    // 只保留hidden=1的
                    return hidden == null || hidden != 1;
                } else {
                    // 只保留hidden=0的
                    return hidden != null && hidden == 1;
                }
            });
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

        // 如果当前用户没有该属性，向上查找父级用户的属性（保留原始的 user_id）
        if (userProperties == null) {
            List<UserGroup> userGroups = getUserGroupService().getByUserId(userId);
            if (!userGroups.isEmpty()) {
                userProperties = findFirstParentProperty(userGroups, key, new ArrayList<>());
            }
        }

        return userProperties;
    }

    @Override
    public List<UserProperties> getAllByUserIdAndKey(Long userId, String key) {
        User user = userService.getById(userId);
        if(user == null){
            return Collections.emptyList();
        }
        List<UserProperties> result = new ArrayList<>();

        // 首先查找当前用户的属性
        LambdaQueryWrapper<UserProperties> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserProperties::getUserId, userId)
                .eq(UserProperties::getKey, key);
        UserProperties userProperties = this.getOne(queryWrapper);
        if (userProperties != null) {
            fillUserType(userProperties);
            result.add(userProperties);
        }

        // 收集所有父级用户的同key属性
        List<UserGroup> userGroups = getUserGroupService().getByUserId(userId);
        if (!userGroups.isEmpty()) {
            collectAllParentProperties(userGroups, key, result, new ArrayList<>());
            // 为父级属性填充 userType
            for (UserProperties property : result) {
                if (property.getUserType() == null) {
                    fillUserType(property);
                }
                if(property.getKey().equals(UserPropertiesConstant.KEY_STORAGE)){
                    // 只有当 storage 的值为 true 时，才将其值设置为 userId
                    if (property.getValue() != null) {
                        String storageValue = new String(property.getValue(), StandardCharsets.UTF_8);
                        if ("true".equalsIgnoreCase(storageValue)) {
                            property.setUserType(1);
                            property.setValue(user.getUniqueId().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<UserProperties> batchGetByUserIdAndKeys(Long userId, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        List<UserProperties> result = new ArrayList<>();

        // 为每个 key 获取所有属性（包括当前用户和父级用户的）
        for (String key : keys) {
            List<UserProperties> allProperties = getAllByUserIdAndKey(userId, key);
            result.addAll(allProperties);
        }

        return result;
    }

    /**
     * 递归查找第一个存在的父级用户属性
     *
     * @param userGroups 当前用户的用户组列表
     * @param key 要查找的属性key
     * @param visitedUsers 已访问的用户ID列表，防止循环引用
     * @return 找到的第一个父级属性，如果未找到则返回null
     */
    private UserProperties findFirstParentProperty(List<UserGroup> userGroups, String key, List<Long> visitedUsers) {
        for (UserGroup userGroup : userGroups) {
            Long parentUserId = userGroup.getParentUserId();
            if (parentUserId != null && !visitedUsers.contains(parentUserId)) {
                visitedUsers.add(parentUserId);

                // 查找父级用户的属性
                UserProperties parentProperty = getDirectUserProperty(parentUserId, key);
                if (parentProperty != null) {
                    // 找到了，直接返回（保留原始的 userId）
                    return parentProperty;
                }

                // 当前父级没有，继续递归查找父级的父级
                List<UserGroup> parentGroups = getUserGroupService().getByUserId(parentUserId);
                if (!parentGroups.isEmpty()) {
                    UserProperties result = findFirstParentProperty(parentGroups, key, new ArrayList<>(visitedUsers));
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
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
            PropertyKeys propertyKey = getPropertyKeysService().getByKey(property.getKey());
            if (propertyKey != null) {
                property.setDataType(propertyKey.getDataType());
                property.setHidden(propertyKey.getHidden());
                property.setScope(propertyKey.getScope());
                property.setDescription(propertyKey.getDescription());
            }
        }
    }

    @Override
    public void fillUserType(UserProperties property) {
        if (property != null && property.getUserId() != null) {
            User user = getUserService().getById(property.getUserId());
            if (user != null) {
                property.setUserType(user.getType());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTemplateProperties(Long templateId,
                                       List<Map<String, Object>> propertyDefaults,
                                       List<String> deleteKeys) {
        if (templateId == null) {
            return;
        }

        Map<String, Object> defaultValues = flattenPropertyDefaults(propertyDefaults);
        Map<String, byte[]> defaultValueBytes = convertAndValidateDefaults(defaultValues);
        boolean needAdd = !defaultValueBytes.isEmpty();

        List<String> keysToDelete = normalizeDeleteKeys(deleteKeys);
        boolean needDelete = !keysToDelete.isEmpty();

        if (!needAdd && !needDelete) {
            return;
        }

        Set<Long> userIds = resolveTemplateUserIds(templateId);
        if (userIds.isEmpty()) {
            return;
        }

        List<UserProperties> existingProperties = this.list(new LambdaQueryWrapper<UserProperties>()
                .in(UserProperties::getUserId, userIds));

        if (needAdd) {
            Map<Long, Map<String, UserProperties>> existingByUser = existingProperties.stream()
                    .collect(Collectors.groupingBy(UserProperties::getUserId,
                            Collectors.toMap(UserProperties::getKey,
                                    Function.identity(),
                                    (origin, replacement) -> origin,
                                    HashMap::new)));

            List<UserProperties> newProperties = new ArrayList<>();
            for (Long userId : userIds) {
                Map<String, UserProperties> userProps = existingByUser.getOrDefault(userId, Collections.emptyMap());
                for (Map.Entry<String, byte[]> entry : defaultValueBytes.entrySet()) {
                    String key = entry.getKey();
                    if (userProps.containsKey(key)) {
                        continue;
                    }
                    UserProperties prop = new UserProperties();
                    prop.setUserId(userId);
                    prop.setKey(key);
                    prop.setValue(entry.getValue());
                    newProperties.add(prop);
                }
            }
            if (!newProperties.isEmpty()) {
                this.saveBatch(newProperties);
            }
        }

        if (needDelete) {
            this.remove(new LambdaQueryWrapper<UserProperties>()
                    .in(UserProperties::getUserId, userIds)
                    .in(UserProperties::getKey, keysToDelete));
        }
    }

    private Map<String, Object> flattenPropertyDefaults(List<Map<String, Object>> propertyDefaults) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        if (propertyDefaults == null) {
            return defaults;
        }
        for (Map<String, Object> entryMap : propertyDefaults) {
            if (entryMap == null || entryMap.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Object> entry : entryMap.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                defaults.put(key.trim(), entry.getValue());
            }
        }
        return defaults;
    }

    private Map<String, byte[]> convertAndValidateDefaults(Map<String, Object> defaults) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        if (defaults == null || defaults.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            byte[] bytes = convertToBytes(entry.getValue());
            long size = bytes == null ? 0 : bytes.length;
            KeyValidationUtils.ValidationResult validationResult = KeyValidationUtils.validateKey(key, size);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.getErrorMessage());
            }
            result.put(key, bytes);
        }
        return result;
    }

    private List<String> normalizeDeleteKeys(List<String> deleteKeys) {
        if (deleteKeys == null) {
            return Collections.emptyList();
        }
        List<String> result = deleteKeys.stream()
                .filter(key -> key != null && !key.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    private Set<Long> resolveTemplateUserIds(Long templateId) {
        String templateIdString = String.valueOf(templateId);
        List<UserProperties> templateBindings = this.list(
                new LambdaQueryWrapper<UserProperties>()
                        .eq(UserProperties::getKey, UserPropertiesConstant.KEY_TEMPLATE_ID)
        );
        Set<Long> userIds = new HashSet<>();
        for (UserProperties binding : templateBindings) {
            if (binding.getUserId() == null || binding.getValue() == null) {
                continue;
            }
            String bindingTemplateId = bytesToString(binding.getValue());
            if (templateIdString.equals(bindingTemplateId)) {
                userIds.add(binding.getUserId());
            }
        }
        return userIds;
    }

    private byte[] convertToBytes(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
    }

    private String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
