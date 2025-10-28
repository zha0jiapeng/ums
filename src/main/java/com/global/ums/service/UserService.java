package com.global.ums.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.entity.User;
import com.global.ums.result.AjaxResult;


/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    
    /**
     * 获取用户信息及其属性
     * 
     * @param id 用户ID
     * @return 包含属性的用户信息
     */
    User getUserWithProperties(Long id);
    
    
    /**
     * 分页获取用户视图对象列表
     *
     * @param page 分页参数
     * @param type 用户类型
     * @param uniqueId 唯一标识
     * @param category 类别（用户属性）
     * @param propertyType 属性类型（用户属性）
     * @return 分页用户视图对象
     */
    Page<User> getUserPage(Page<User> page, Integer type, String uniqueId, String category, String propertyType);

    AjaxResult addUser(User user);
} 