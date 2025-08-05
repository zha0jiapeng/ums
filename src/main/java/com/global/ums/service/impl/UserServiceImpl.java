package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.entity.User;
import com.global.ums.entity.UserProperties;
import com.global.ums.mapper.UserMapper;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.PasswordService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.service.UserService;
import com.global.ums.utils.UserTypeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Autowired
    private UserPropertiesService userPropertiesService;

    @Autowired
    private PasswordService passwordService;

    @Value("${user.default-password:123456}")
    private String defaultPassword;
    
    @Override
    public User getUserWithProperties(Long id) {
        // 获取用户基本信息
        User user = this.getById(id);
        if (user != null) {
            // 获取用户属性列表
            List<UserProperties> properties = userPropertiesService.getByUserId(id);
            user.setProperties(properties);
            user.setTypeDesc(UserTypeUtils.getTypeDesc(user.getType()));
        }
        return user;
    }
    
    @Override
    public Page<User> getUserPage(Page<User> page, Integer type, String uniqueId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        if (type != null) {
            queryWrapper.eq(User::getType, type);
        }
        
        if (uniqueId != null && !uniqueId.isEmpty()) {
            queryWrapper.like(User::getUniqueId, uniqueId);
        }
        
        Page<User> userPage = this.page(page, queryWrapper);
        userPage.getRecords().forEach(user -> {
            // 获取用户属性列表
            List<UserProperties> properties = userPropertiesService.getByUserId(user.getId());
            user.setProperties(properties);
            user.setTypeDesc(UserTypeUtils.getTypeDesc(user.getType()));
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
            if( user.getPassword()==null) {
                passwordService.setPassword(user.getId(), defaultPassword );
            }else{
                if(user.getPassword().length()<6) {
                    return AjaxResult.errorI18n("user.password.too.simple");
                }
                passwordService.setPassword(user.getId(), user.getPassword() );
            }
            return AjaxResult.successI18n("user.add.success");
        }else {
            return AjaxResult.errorI18n("user.add.error");
        }
    }
} 