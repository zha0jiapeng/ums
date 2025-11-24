package com.global.ums.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.dto.PropertyTreeDTO;
import com.global.ums.dto.UserInfoTreeDTO;
import com.global.ums.dto.UserTreeNodeDTO;
import com.global.ums.entity.User;
import com.global.ums.result.AjaxResult;

import java.util.List;


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
     * 获取包含父节点属性合并后的用户信息
     *
     * @param id 用户ID
     * @return 合并属性后的用户信息
     */
    User getUserWithInheritedProperties(Long id);
    
    
    /**
     * 分页获取用户视图对象列表
     *
     * @param page 分页参数
     * @param type 用户类型
     * @param uniqueId 唯一标识
     * @param parentId 上级用户ID
     * @param groupType 用户组类型（1:部门 2:应用，仅当type=2时有效）
     * @return 分页用户视图对象
     */
    Page<User> getUserPage(Page<User> page, Integer type, String uniqueId, Long parentId, Integer groupType);

    AjaxResult addUser(User user);

    /**
     * 删除用户（包含引用检查和属性级联删除）
     *
     * @param id 用户ID
     * @return 删除结果
     */
    AjaxResult deleteUser(Long id);


    /**
     * 获取用户与父节点的树状结构（包含各节点属性）
     *
     * @param userId 用户ID
     * @return 树状信息
     */
    UserInfoTreeDTO getUserInfoTree(Long userId);

    /**
     * 根据类型获取用户树
     *
     * @param type 用户类型
     * @return 树状用户列表
     */
    List<UserTreeNodeDTO> getUserTreeByType(Integer type);

    /**
     * 将用户关联到默认部门
     * 如果默认部门不存在则创建
     *
     * @param userId 用户ID
     */
    void associateToDefaultDepartment(Long userId);
}