package com.global.ums.service.impl;

import com.global.ums.constant.UserPropertiesConstant;
import com.global.ums.entity.UserProperties;
import com.global.ums.service.PasswordService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 密码服务实现
 */
@Service
public class PasswordServiceImpl implements PasswordService {

    @Autowired
    private UserPropertiesService userPropertiesService;

    /**
     * 设置用户密码
     */
    @Override
    public boolean setPassword(Long userId, String password) {
        // 生成随机盐
        String salt = PasswordUtils.generateSalt();
        
        // 加密密码
        String encryptedPassword = PasswordUtils.encryptPassword(password, salt);
        
        // 转换为字节数组
        byte[] passwordBytes = PasswordUtils.toBytes(encryptedPassword, salt);
        
        // 查询是否已有密码
        UserProperties passwordProp = userPropertiesService.getByUserIdAndKey(userId, UserPropertiesConstant.KEY_PASSWORD);
        
        if (passwordProp == null) {
            // 创建新密码属性
            passwordProp = new UserProperties();
            passwordProp.setUserId(userId);
            passwordProp.setKey(UserPropertiesConstant.KEY_PASSWORD);
            passwordProp.setValue(passwordBytes);
            
            return userPropertiesService.save(passwordProp);
        } else {
            // 更新密码
            passwordProp.setValue(passwordBytes);
            return userPropertiesService.updateById(passwordProp);
        }
    }


    /**
     * 验证用户密码
     */
    @Override
    public boolean verifyPassword(Long userId, String password) {
        // 获取密码属性
        UserProperties passwordProp = userPropertiesService.getByUserIdAndKey(userId, UserPropertiesConstant.KEY_PASSWORD);
        
        if (passwordProp == null || passwordProp.getValue() == null) {
            return false;
        }
        
        // 解析密码和盐
        String[] passwordAndSalt = PasswordUtils.fromBytes(passwordProp.getValue());
        if (passwordAndSalt.length != 2) {
            return false;
        }
        
        String encryptedPassword = passwordAndSalt[0];
        String salt = passwordAndSalt[1];
        
        // 验证密码
        return PasswordUtils.verifyPassword(password, salt, encryptedPassword);
    }

    /**
     * 修改用户密码
     */
    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        // 验证旧密码是否正确
        if (!verifyPassword(userId, oldPassword)) {
            return false;
        }
        
        // 设置新密码
        return setPassword(userId, newPassword);
    }
} 