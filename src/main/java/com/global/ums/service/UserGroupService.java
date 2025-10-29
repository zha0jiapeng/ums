package com.global.ums.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.entity.UserGroup;
import com.global.ums.result.AjaxResult;

import java.util.List;

/**
 * 用户组服务接口
 */
public interface UserGroupService extends IService<UserGroup> {
    
    /**
     * 根据用户ID获取用户组
     *
     * @param userId 用户ID
     * @return 用户组信息
     */
    List<UserGroup> getByUserId(Long userId);
    
    /**
     * 根据上级用户ID获取用户组列表
     *
     * @param parentUserId 上级用户ID
     * @return 用户组列表
     */
    List<UserGroup> getByParentUserId(Long parentUserId);
    
    /**
     * 获取用户组详情（包含用户信息）
     *
     * @param id 用户组ID
     * @return 用户组详情
     */
    UserGroup getUserGroupDetail(Long id);
    
    /**
     * 分页查询用户组
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param parentUserId 上级用户ID
     * @return 分页用户组列表
     */
    Page<UserGroup> getUserGroupPage(Page<UserGroup> page, Long userId, Long parentUserId);
    
    /**
     * 添加用户组关系
     *
     * @param userId 用户ID
     * @param parentUserId 上级用户ID
     * @return 是否成功
     */
    boolean addUserGroup(Long userId, Long parentUserId);
    
    /**
     * 验证是否可以添加用户组关系
     *
     * @param userId 用户ID
     * @param parentUserId 上级用户ID
     * @return 是否可以添加
     */
    boolean validateUserGroup(Long userId, Long parentUserId);
    
    /**
     * 批量添加用户组关系
     *
     * @param userGroups 用户组关系列表
     * @return 添加结果
     */
    AjaxResult batchAddUserGroups(List<UserGroup> userGroups);
} 