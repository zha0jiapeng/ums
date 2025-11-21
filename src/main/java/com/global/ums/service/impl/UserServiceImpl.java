package com.global.ums.service.impl;

import cn.hutool.core.util.ByteUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.constant.UserPropertiesConstant;
import com.global.ums.dto.PropertyTreeDTO;
import com.global.ums.dto.UserInfoTreeDTO;
import com.global.ums.dto.UserTreeNodeDTO;
import com.global.ums.entity.User;
import com.global.ums.entity.Template;
import com.global.ums.entity.UserGroup;
import com.global.ums.entity.UserProperties;
import com.global.ums.entity.PropertyKeys;
import com.global.ums.enums.UserType;
import com.global.ums.mapper.UserMapper;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.PasswordService;
import com.global.ums.service.PropertyKeysService;
import com.global.ums.service.TemplateService;
import com.global.ums.service.UserGroupService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.service.UserService;
import com.global.ums.utils.KeyValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private UserPropertiesService userPropertiesService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private PropertyKeysService propertyKeysService;

    @Value("${user.default-password:123456}")
    private String defaultPassword;
    
    @Override
    public User getUserWithProperties(Long id) {
        // 获取用户基本信息
        User user = this.getById(id);
        if (user != null) {
            // 获取用户属性列表
            List<UserProperties> properties = userPropertiesService.getByUserId(id,false);

            // 为每个属性填充配置信息和用户类型
            if (properties != null && !properties.isEmpty()) {
                properties.forEach(prop -> {
                    userPropertiesService.fillPropertyKeysInfo(prop);
                    userPropertiesService.fillUserType(prop);
                });
            }

            user.setProperties(properties);
            user.setTypeDesc(UserType.fromValue(user.getType()).getDescription());
            
            // 获取dept和application用户列表
           // loadDeptAndApplicationUsers(user);
        }
        return user;
    }

    @Override
    public User getUserWithInheritedProperties(Long id) {
        User user = getUserWithProperties(id);
        if (user == null) {
            return null;
        }

        List<UserProperties> mergedProperties = new ArrayList<>();
        // 收集当前用户拥有的 key，用于判断是否覆盖父集
        Set<String> userOwnedKeys = new HashSet<>();

        if (user.getProperties() != null) {
            for (UserProperties property : user.getProperties()) {
                addPropertyIfNotDuplicate(mergedProperties, property);
                userOwnedKeys.add(property.getKey());
            }
        }

        List<User> parentUsers = getAllParentUsers(id);
        for (User parentUser : parentUsers) {
            if (parentUser.getProperties() == null) {
                continue;
            }
            for (UserProperties parentProperty : parentUser.getProperties()) {
                // 检查该 key 是否配置了覆盖父集属性
                String key = parentProperty.getKey();
                PropertyKeys propertyKey = propertyKeysService.getByKey(key);
                boolean shouldOverrideParent = propertyKey != null
                        && propertyKey.getOverrideParent() != null
                        && propertyKey.getOverrideParent() == 1;

                // 如果用户自己有这个 key 且配置为覆盖父集，则跳过父级的该属性
                if (shouldOverrideParent && userOwnedKeys.contains(key)) {
                    continue;
                }

                if(parentProperty.getKey().equals(UserPropertiesConstant.KEY_STORAGE)){
                    // 只有当 storage 的值为 true 时，才将其值设置为 userId
                    if (parentProperty.getValue() != null) {
                        String storageValue = new String(parentProperty.getValue(), StandardCharsets.UTF_8);
                        if ("true".equalsIgnoreCase(storageValue)) {
                            parentProperty.setValue(id.toString().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
                addPropertyIfNotDuplicate(mergedProperties, parentProperty);
            }
        }

        user.setProperties(mergedProperties);
        return user;
    }

    /**
     * 将属性添加到列表中，如果已存在相同key且value相同的属性，则不重复添加
     */
    private void addPropertyIfNotDuplicate(List<UserProperties> properties, UserProperties propertyToAdd) {
        if (propertyToAdd == null) {
            return;
        }

        for (UserProperties existing : properties) {
            if (isSameKeyAndValue(existing, propertyToAdd)) {
                return;
            }
        }
        properties.add(propertyToAdd);
    }

    private boolean isSameKeyAndValue(UserProperties first, UserProperties second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.getKey() == null || second.getKey() == null) {
            return false;
        }
        if (!first.getKey().equals(second.getKey())) {
            return false;
        }

        byte[] firstValue = first.getValue();
        byte[] secondValue = second.getValue();

        if (firstValue == null && secondValue == null) {
            return true;
        }
        if (firstValue == null || secondValue == null) {
            return false;
        }

        return Arrays.equals(firstValue, secondValue);
    }

    private List<User> getAllParentUsers(Long userId) {
        List<User> parents = new ArrayList<>();
        collectParentUsers(userId, parents, new HashSet<>());
        return parents;
    }

    private void collectParentUsers(Long userId, List<User> parents, Set<Long> visited) {
        if (userId == null || visited.contains(userId)) {
            return;
        }
        visited.add(userId);

        List<UserGroup> userGroups = getUserGroupService().getByUserId(userId);
        if (userGroups == null || userGroups.isEmpty()) {
            return;
        }

        for (UserGroup userGroup : userGroups) {
            Long parentId = userGroup.getParentUserId();
            if (parentId == null || visited.contains(parentId)) {
                continue;
            }
            User parent = getUserWithProperties(parentId);
            if (parent != null) {
                parents.add(parent);
            }
            collectParentUsers(parentId, parents, visited);
        }
    }
    
    @Override
    public UserInfoTreeDTO getUserInfoTree(Long userId) {
        if (userId == null) {
            return null;
        }
        return buildUserInfoTree(userId, new HashSet<>());
    }

    private UserInfoTreeDTO buildUserInfoTree(Long userId, Set<Long> path) {
        if (userId == null || path.contains(userId)) {
            return null;
        }

        path.add(userId);
        User user = getUserWithProperties(userId);
        if (user == null) {
            path.remove(userId);
            return null;
        }

        UserInfoTreeDTO node = convertToTreeNode(user);
        List<UserGroup> relations = getUserGroupService().getByUserId(userId);
        if (relations != null && !relations.isEmpty()) {
            List<UserInfoTreeDTO> parents = new ArrayList<>();
            for (UserGroup relation : relations) {
                UserInfoTreeDTO parentNode = buildUserInfoTree(relation.getParentUserId(), path);
                if (parentNode != null) {
                    parents.add(parentNode);
                }
            }
            if (!parents.isEmpty()) {
                node.setParents(parents);
            }
        }

        path.remove(userId);
        return node;
    }

    private UserInfoTreeDTO convertToTreeNode(User user) {
        UserInfoTreeDTO node = new UserInfoTreeDTO();
        node.setUserId(user.getId());
        node.setUniqueId(user.getUniqueId());
        node.setType(user.getType());
        node.setTypeDesc(user.getTypeDesc());
        node.setProperties(user.getProperties());
        return node;
    }

    @Override
    public List<UserTreeNodeDTO> getUserTreeByType(Integer type) {
        if (type == null) {
            return new ArrayList<>();
        }

        List<User> users = this.list(new LambdaQueryWrapper<User>().eq(User::getType, type));
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> userIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<UserProperties> propWrapper = new LambdaQueryWrapper<>();
        propWrapper.in(UserProperties::getUserId, userIds);
        List<UserProperties> allProperties = userPropertiesService.list(propWrapper);

        Map<Long, List<UserProperties>> propertiesMap = new HashMap<>();
        for (UserProperties property : allProperties) {
            userPropertiesService.fillPropertyKeysInfo(property);
            Integer hidden = property.getHidden();
            if (hidden != null && hidden == 1) {
                continue;
            }
            propertiesMap.computeIfAbsent(property.getUserId(), id -> new ArrayList<>()).add(property);
        }

        Map<Long, UserTreeNodeDTO> nodeMap = new HashMap<>();
        for (User user : users) {
            UserTreeNodeDTO node = new UserTreeNodeDTO();
            node.setUserId(user.getId());
            node.setUniqueId(user.getUniqueId());
            node.setType(user.getType());
            node.setTypeDesc(UserType.fromValue(user.getType()).getDescription());
            List<UserProperties> props = propertiesMap.get(user.getId());
            node.setProperties(props != null ? props : new ArrayList<>());
            nodeMap.put(user.getId(), node);
        }

        LambdaQueryWrapper<UserGroup> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.in(UserGroup::getUserId, userIds)
                .in(UserGroup::getParentUserId, userIds);
        List<UserGroup> relations = userGroupService.list(relationWrapper);

        Map<Long, List<UserTreeNodeDTO>> childrenBuffer = new HashMap<>();
        Set<Long> childrenWithParent = new HashSet<>();
        for (UserGroup relation : relations) {
            Long parentId = relation.getParentUserId();
            Long childId = relation.getUserId();
            if (parentId == null || childId == null) {
                continue;
            }
            UserTreeNodeDTO parentNode = nodeMap.get(parentId);
            UserTreeNodeDTO childNode = nodeMap.get(childId);
            if (parentNode == null || childNode == null) {
                continue;
            }
            childrenBuffer.computeIfAbsent(parentId, id -> new ArrayList<>()).add(childNode);
            childrenWithParent.add(childId);
        }

        for (Map.Entry<Long, List<UserTreeNodeDTO>> entry : childrenBuffer.entrySet()) {
            UserTreeNodeDTO parentNode = nodeMap.get(entry.getKey());
            if (parentNode != null) {
                parentNode.setChildren(entry.getValue());
            }
        }

        return nodeMap.values().stream()
                .filter(node -> !childrenWithParent.contains(node.getUserId()))
                .collect(Collectors.toList());
    }

    /**
     * 标记当前事务回滚并返回结果
     */
    private AjaxResult rollbackAndReturn(AjaxResult result) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
    }

    /**
     * 加载用户的dept和application用户列表
     */
    private void loadDeptAndApplicationUsers(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        
        List<User> deptUsers = new ArrayList<>();
        List<User> applicationUsers = new ArrayList<>();
        
        // 获取该用户的直接上级
        List<UserGroup> userGroups = getUserGroupService().getByUserId(user.getId());
        
        for (UserGroup ug : userGroups) {
            Long parentId = ug.getParentUserId();
            if (parentId != null) {
                User parentUser = this.getById(parentId);
                if (parentUser != null && parentUser.getType() == 2) {  // type=2 为用户组
                    // 检查该用户的category属性
                    List<UserProperties> parentProps = userPropertiesService.getByUserId(parentId, false);
                    String category = null;
                    
                    for (UserProperties prop : parentProps) {
                        if ("category".equals(prop.getKey()) && prop.getValue() != null) {
                            category = new String(prop.getValue());
                            break;
                        }
                    }
                    
                    // 根据category分类
                    if ("dept".equals(category)) {
                        // 这是一个dept用户，继续获取它的上级application用户
                        deptUsers.add(parentUser);
                        
                        // 获取dept的上级application用户
                        List<UserGroup> deptGroups = getUserGroupService().getByUserId(parentId);
                        for (UserGroup deptGroup : deptGroups) {
                            Long appParentId = deptGroup.getParentUserId();
                            if (appParentId != null) {
                                User appParent = this.getById(appParentId);
                                if (appParent != null && appParent.getType() == 3) {
                                    // 检查是否为application
                                    List<UserProperties> appProps = userPropertiesService.getByUserId(appParentId, false);
                                    String appCategory = null;
                                    for (UserProperties prop : appProps) {
                                        if ("category".equals(prop.getKey()) && prop.getValue() != null) {
                                            appCategory = new String(prop.getValue());
                                            break;
                                        }
                                    }
                                    if ("application".equals(appCategory)) {
                                        applicationUsers.add(appParent);
                                    }
                                }
                            }
                        }
                    } else if ("application".equals(category)) {
                        // 直接是application用户
                        applicationUsers.add(parentUser);
                    }
                }
            }
        }
        
        user.setDepts(deptUsers);
        user.setApplications(applicationUsers);
    }
    
    @Override
    public Page<User> getUserPage(Page<User> page, Integer type, String uniqueId, Long parentId, Integer groupType) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        if (type != null) {
            queryWrapper.eq(User::getType, type);
        }

        if (uniqueId != null && !uniqueId.isEmpty()) {
            queryWrapper.like(User::getUniqueId, uniqueId);
        }

        // 如果指定了 groupType，根据 group_type 属性筛选用户组
        if (groupType != null) {
            // 查找所有拥有 group_type 属性且值匹配的用户ID
            LambdaQueryWrapper<UserProperties> propWrapper = new LambdaQueryWrapper<>();
            propWrapper.eq(UserProperties::getKey, UserPropertiesConstant.KEY_GROUP_TYPE)
                    .eq(UserProperties::getValue, String.valueOf(groupType).getBytes());
            List<UserProperties> groupTypeProps = userPropertiesService.list(propWrapper);

            if (groupTypeProps == null || groupTypeProps.isEmpty()) {
                // 如果没有符合条件的用户，返回空结果
                return new Page<>(page.getCurrent(), page.getSize());
            }

            List<Long> userIds = groupTypeProps.stream()
                    .map(UserProperties::getUserId)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            queryWrapper.in(User::getId, userIds);
        }

        // 如果指定了 parentId，根据 user_group 表过滤子用户
        if (parentId != null) {
            List<UserGroup> childGroups = userGroupService.getByParentUserId(parentId);
            if (childGroups == null || childGroups.isEmpty()) {
                // 如果该上级用户没有子用户，直接返回空结果
                return new Page<>(page.getCurrent(), page.getSize());
            }
            List<Long> childUserIds = childGroups.stream()
                    .map(UserGroup::getUserId)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            queryWrapper.in(User::getId, childUserIds);
        }

        Page<User> userPage = this.page(page, queryWrapper);
        userPage.getRecords().forEach(user -> {
            // 获取用户属性列表
            List<UserProperties> properties = userPropertiesService.getByUserId(user.getId(),null);

            // 为每个属性填充配置信息和用户类型
            if (properties != null && !properties.isEmpty()) {
                properties.forEach(prop -> {
                    userPropertiesService.fillPropertyKeysInfo(prop);
                    userPropertiesService.fillUserType(prop);
                });
            }

            user.setProperties(properties);
            user.setTypeDesc(UserType.fromValue(user.getType()).getDescription());
        });
        return userPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult addUser(User user) {
        long count = count(new LambdaQueryWrapper<User>().eq(User::getUniqueId, user.getUniqueId()));
        if(count>0){
            return rollbackAndReturn(AjaxResult.errorI18n("user.register.exists"));
        }
        boolean save = save(user);
        if (save) {
            if(user.getType()==1) {
                if (user.getPassword() == null) {
                    passwordService.setPassword(user.getId(), defaultPassword);
                } else {
                    if (user.getPassword().length() < 6) {

                        return rollbackAndReturn(AjaxResult.errorI18n("user.password.too.simple"));
                    }
                    passwordService.setPassword(user.getId(), user.getPassword());
                }
            }else if(user.getType()==2){
                // 校验所有属性的 key
                if (user.getProperties() != null && !user.getProperties().isEmpty()) {
                    for (UserProperties prop : user.getProperties()) {
                        String key = prop.getKey();
                        // 获取属性值的字节大小
                        long valueSize = prop.getValue() != null ? prop.getValue().length : 0;

                        // 校验 key 和大小
                        KeyValidationUtils.ValidationResult validationResult =
                            KeyValidationUtils.validateKey(key, valueSize);

                        if (!validationResult.isValid()) {
                            // 校验失败，返回错误信息
                            return rollbackAndReturn(AjaxResult.error(validationResult.getErrorMessage()));
                        }
                    }

                    // 查找 templateId 属性
                    Long templateId = null;
                    for (UserProperties prop : user.getProperties()) {
                        if (UserPropertiesConstant.KEY_TEMPLATE_ID.equals(prop.getKey()) && prop.getValue() != null) {
                            try {
                                String templateIdStr = new String(prop.getValue(), StandardCharsets.UTF_8);
                                templateId = Long.parseLong(templateIdStr);
                                break;
                            } catch (NumberFormatException e) {
                                // 如果解析失败，忽略
                            }
                        }
                    }

                    // 如果找到 templateId，自动添加 group_type 属性
                    if (templateId != null) {
                        Template template = templateService.getById(templateId);
                        if (template != null && template.getType() != null) {
                            // 检查是否已存在 group_type 属性
                            boolean hasGroupType = false;
                            for (UserProperties prop : user.getProperties()) {
                                if (UserPropertiesConstant.KEY_GROUP_TYPE.equals(prop.getKey())) {
                                    hasGroupType = true;
                                    break;
                                }
                            }

                            // 如果不存在，自动添加 group_type 属性
                            if (!hasGroupType) {
                                UserProperties groupTypeProp = new UserProperties();
                                groupTypeProp.setKey(UserPropertiesConstant.KEY_GROUP_TYPE);
                                groupTypeProp.setValue(String.valueOf(template.getType()).getBytes(StandardCharsets.UTF_8));
                                user.getProperties().add(groupTypeProp);
                            }
                        }
                    }

                    // 校验通过，设置 userId 并保存
                    user.getProperties().forEach(prop -> {
                        prop.setUserId(user.getId());
                    });
                    userPropertiesService.saveBatch(user.getProperties());
                }

                if(user.getParentId()!=null){
                    userGroupService.addUserGroup(user.getId(), user.getParentId());
                }
            }
            return AjaxResult.successI18n("user.add.success");
        }else {
            return rollbackAndReturn(AjaxResult.errorI18n("user.add.error"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult deleteUser(Long id) {
        // 1. 检查用户是否存在
        User user = this.getById(id);
        if (user == null) {
            return rollbackAndReturn(AjaxResult.errorI18n("user.not.found"));
        }

        // 2. 检查用户是否在 user_group 表中被引用为上级用户
        List<UserGroup> childGroups = userGroupService.getByParentUserId(id);
        if (childGroups != null && !childGroups.isEmpty()) {
            return rollbackAndReturn(AjaxResult.errorI18n("user.delete.has.children"));
        }

        // 3. 删除用户属性
        LambdaQueryWrapper<UserProperties> propertiesWrapper = new LambdaQueryWrapper<>();
        propertiesWrapper.eq(UserProperties::getUserId, id);
        userPropertiesService.remove(propertiesWrapper);

        // 4. 删除用户在 user_group 表中的记录（作为子用户）
        LambdaQueryWrapper<UserGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.eq(UserGroup::getUserId, id);
        userGroupService.remove(groupWrapper);

        // 5. 删除用户
        boolean result = this.removeById(id);
        if (result) {
            return AjaxResult.successI18n("user.delete.success");
        } else {
            return rollbackAndReturn(AjaxResult.errorI18n("user.delete.error"));
        }
    }
    /**
     * 为单个根用户构建category树（只包含用户节点，不包含属性）
     */
    private PropertyTreeDTO buildCategoryTree(Long userId, List<Long> categoryUserIds, Set<Long> processedUsers) {
        if (processedUsers.contains(userId)) {
            return null;
        }
        processedUsers.add(userId);

        User user = getById(userId);
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
            if (categoryUserIds.contains(childUserId)) {
                PropertyTreeDTO childNode = buildCategoryTree(childUserId, categoryUserIds, processedUsers);
                if (childNode != null) {
                    userNode.getChildren().add(childNode);
                }
            }
        }

        return userNode;
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
} 
