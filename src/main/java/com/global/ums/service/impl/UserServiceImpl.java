package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.constant.UserPropertiesConstant;
import com.global.ums.dto.PropertyTreeDTO;
import com.global.ums.entity.User;
import com.global.ums.entity.UserGroup;
import com.global.ums.entity.UserProperties;
import com.global.ums.enums.UserType;
import com.global.ums.mapper.UserMapper;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.PasswordService;
import com.global.ums.service.UserGroupService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    @Value("${user.default-password:123456}")
    private String defaultPassword;
    
    @Override
    public User getUserWithProperties(Long id) {
        // 获取用户基本信息
        User user = this.getById(id);
        if (user != null) {
            // 获取用户属性列表
            List<UserProperties> properties = userPropertiesService.getByUserId(id,false);

            // 为每个属性填充 dataType（从 ums_property_keys 表查询）
            if (properties != null && !properties.isEmpty()) {
                properties.forEach(prop -> {
                    userPropertiesService.fillPropertyKeysInfo(prop);
                });
            }

            user.setProperties(properties);
            user.setTypeDesc(UserType.fromValue(user.getType()).getDescription());
            
            // 获取dept和application用户列表
            loadDeptAndApplicationUsers(user);
        }
        return user;
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
                if (parentUser != null && parentUser.getType() == 3) {  // type=3 为用户组
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
    public Page<User> getUserPage(Page<User> page, Integer type, String uniqueId, Long parentId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        if (type != null) {
            queryWrapper.eq(User::getType, type);
        }

        if (uniqueId != null && !uniqueId.isEmpty()) {
            queryWrapper.like(User::getUniqueId, uniqueId);
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

            // 为每个属性填充 dataType（从 ums_property_keys 表查询）
            if (properties != null && !properties.isEmpty()) {
                properties.forEach(prop -> {
                    userPropertiesService.fillPropertyKeysInfo(prop);
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
            return AjaxResult.errorI18n("user.register.exists");
        }
        boolean save = save(user);
        if (save) {
            if(user.getType()==1) {
                if (user.getPassword() == null) {
                    passwordService.setPassword(user.getId(), defaultPassword);
                } else {
                    if (user.getPassword().length() < 6) {
                        return AjaxResult.errorI18n("user.password.too.simple");
                    }
                    passwordService.setPassword(user.getId(), user.getPassword());
                }
            }else if(user.getType()==3){
                user.getProperties().forEach(prop -> {
                    prop.setUserId(user.getId());
                });
                userPropertiesService.saveBatch(user.getProperties());
                if(user.getParentId()!=null){
                    userGroupService.addUserGroup(user.getId(), user.getParentId());
                }
            }
            return AjaxResult.successI18n("user.add.success");
        }else {
            return AjaxResult.errorI18n("user.add.error");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult deleteUser(Long id) {
        // 1. 检查用户是否存在
        User user = this.getById(id);
        if (user == null) {
            return AjaxResult.errorI18n("user.not.found");
        }

        // 2. 检查用户是否在 user_group 表中被引用为上级用户
        List<UserGroup> childGroups = userGroupService.getByParentUserId(id);
        if (childGroups != null && !childGroups.isEmpty()) {
            return AjaxResult.errorI18n("user.delete.has.children");
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
            return AjaxResult.errorI18n("user.delete.error");
        }
    }

    @Override
    public List<PropertyTreeDTO> getTree(Long userId, String category) {
        // 参数验证
        if (category == null || category.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 1. 获取所有指定category的用户ID集合
        List<Long> categoryUserIds = userPropertiesService.list(new LambdaQueryWrapper<UserProperties>()
                .eq(UserProperties::getKey, "category"))
                .stream()
                .filter(prop -> prop.getValue() != null && category.equals(new String(prop.getValue())))
                .map(UserProperties::getUserId)
                .distinct()
                .collect(Collectors.toList());

        if (categoryUserIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 获取所有user_group中的用户关系
        List<UserGroup> allUserGroups = getUserGroupService().list();

        // 从user_group中的parent_user_id集合（即有父级且父级在category集合中的用户）
        Set<Long> usersWithParentInCategory = allUserGroups.stream()
                .filter(ug -> categoryUserIds.contains(ug.getParentUserId()))
                .map(UserGroup::getUserId)
                .collect(Collectors.toSet());

        // 3. 找出category用户中真正的根（在category集合中没有父级的）
        List<Long> rootUserIds = categoryUserIds.stream()
                .filter(id -> !usersWithParentInCategory.contains(id))
                .collect(Collectors.toList());

        // 4. 为每个根用户构建树形结构
        List<PropertyTreeDTO> result = new ArrayList<>();
        Set<Long> processedUsers = new HashSet<>();

        for (Long rootUserId : rootUserIds) {
            PropertyTreeDTO rootNode = buildCategoryTree(rootUserId, categoryUserIds, processedUsers);
            if (rootNode != null) {
                result.add(rootNode);
            }
        }

        return result;
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