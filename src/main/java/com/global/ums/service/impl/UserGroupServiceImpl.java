package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.constant.UserTypeConstant;
import com.global.ums.entity.User;
import com.global.ums.entity.UserGroup;
import com.global.ums.mapper.UserGroupMapper;
import com.global.ums.service.UserGroupService;
import com.global.ums.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户组服务实现类
 */
@Service
public class UserGroupServiceImpl extends ServiceImpl<UserGroupMapper, UserGroup> implements UserGroupService {
    
    private final ApplicationContext applicationContext;
    private UserService userService;
    
    public UserGroupServiceImpl(ApplicationContext applicationContext) {
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
    
    @Override
    public List<UserGroup> getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        LambdaQueryWrapper<UserGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserGroup::getUserId, userId);
        return this.list(queryWrapper);
    }
    
    @Override
    public List<UserGroup> getByParentUserId(Long parentUserId) {
        if (parentUserId == null) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<UserGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserGroup::getParentUserId, parentUserId);
        return this.list(queryWrapper);
    }
    
    @Override
    public UserGroup getUserGroupDetail(Long id) {
        UserGroup userGroup = this.getById(id);
        if (userGroup != null) {
            // 获取用户信息
            User user = getUserService().getById(userGroup.getUserId());
            userGroup.setUser(user);
            
            // 获取上级用户信息
            User parentUser = getUserService().getById(userGroup.getParentUserId());
            userGroup.setParentUser(parentUser);
        }
        return userGroup;
    }
    
    @Override
    public Page<UserGroup> getUserGroupPage(Page<UserGroup> page, Long userId, Long parentUserId) {
        LambdaQueryWrapper<UserGroup> queryWrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            queryWrapper.eq(UserGroup::getUserId, userId);
        }
        
        if (parentUserId != null) {
            queryWrapper.eq(UserGroup::getParentUserId, parentUserId);
        }
        
        Page<UserGroup> result = this.page(page, queryWrapper);
        
        // 填充用户信息
        for (UserGroup userGroup : result.getRecords()) {
            User user = getUserService().getById(userGroup.getUserId());
            userGroup.setUser(user);
            
            User parentUser = getUserService().getById(userGroup.getParentUserId());
            userGroup.setParentUser(parentUser);
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUserGroup(Long userId, Long parentUserId) {
        // 验证是否可以添加用户组关系
        if (!validateUserGroup(userId, parentUserId)) {
            return false;
        }
        
        // 创建用户组实体
        UserGroup userGroup = new UserGroup();
        userGroup.setUserId(userId);
        userGroup.setParentUserId(parentUserId);
        
        return this.save(userGroup);
    }
    
    @Override
    public boolean validateUserGroup(Long userId, Long parentUserId) {
        if (userId == null || parentUserId == null) {
            return false;
        }
        
        // 验证用户是否存在
        User user = getUserService().getById(userId);
        if (user == null) {
            return false;
        }
        
        // 验证上级用户是否存在
        User parentUser = getUserService().getById(parentUserId);
        if (parentUser == null) {
            return false;
        }
        
        // 验证上级用户是否为组类型
        if (parentUser.getType() == null || parentUser.getType() != UserTypeConstant.USER_GROUP) {
            return false;
        }
        
        // 验证用户是否已在组中
        List<UserGroup> existingGroup = getByUserId(userId);
        return existingGroup.size() == 0;
    }
} 