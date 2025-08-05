package com.global.ums.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.global.ums.annotation.RequireAuth;
import com.global.ums.entity.UserGroup;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UserGroupService;
import com.global.ums.utils.LoginUserContextHolder;
import com.global.ums.utils.MessageUtils;
import com.global.ums.utils.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户组控制器
 */
@RestController
@RequestMapping("/userGroup")
@RequireAuth
public class UserGroupController {

    @Autowired
    private UserGroupService userGroupService;

    /**
     * 添加用户组关系
     */
    @PostMapping("/add")
    public AjaxResult add(@RequestBody UserGroup userGroup) {
        // 验证添加条件
        boolean valid = userGroupService.validateUserGroup(userGroup.getUserId(), userGroup.getParentUserId());
        if (!valid) {
            return AjaxResult.errorI18n("user.group.add.error");
        }
        
        boolean result = userGroupService.save(userGroup);
        if (result) {
            return AjaxResult.success(SpringUtils.getBean(MessageUtils.class).getMessage("user.group.add.success"), userGroup);
        } else {
            return AjaxResult.errorI18n("user.group.add.error.general");
        }
    }

    /**
     * 删除用户组关系
     */
    @DeleteMapping("/delete/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        boolean result = userGroupService.removeById(id);
        if (result) {
            return AjaxResult.successI18n("user.group.delete.success");
        } else {
            return AjaxResult.errorI18n("user.group.delete.error");
        }
    }

    /**
     * 更新用户组关系
     */
    @PutMapping("/update")
    public AjaxResult update(@RequestBody UserGroup userGroup) {
        boolean result = userGroupService.updateById(userGroup);
        if (result) {
            return AjaxResult.successI18n("user.group.update.success");
        } else {
            return AjaxResult.errorI18n("user.group.update.error");
        }
    }

    /**
     * 获取用户组详情
     */
    @GetMapping("/info/{id}")
    public AjaxResult info(@PathVariable Long id) {
        UserGroup userGroup = userGroupService.getUserGroupDetail(id);
        if (userGroup != null) {
            return AjaxResult.success(userGroup);
        } else {
            return AjaxResult.errorI18n("user.group.detail.not.found");
        }
    }

    /**
     * 根据用户ID获取用户组
     */
    @GetMapping("/byUserId")
    public AjaxResult getByUserId() {
        Long userId = LoginUserContextHolder.getUserId();
        List<UserGroup> userGroup = userGroupService.getByUserId(userId);
        if (userGroup != null) {
            return AjaxResult.success(userGroup);
        } else {
            return AjaxResult.errorI18n("user.group.not.found");
        }
    }

    /**
     * 根据上级用户ID获取用户组列表
     */
    @GetMapping("/byParentUserId/{parentUserId}")
    public AjaxResult getByParentUserId(@PathVariable Long parentUserId) {
        List<UserGroup> userGroups = userGroupService.getByParentUserId(parentUserId);
        return AjaxResult.success(userGroups);
    }

    /**
     * 分页查询用户组
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") Integer pageNum,
                         @RequestParam(defaultValue = "10") Integer pageSize,
                         @RequestParam(required = false) Long userId,
                         @RequestParam(required = false) Long parentUserId) {
        Page<UserGroup> page = new Page<>(pageNum, pageSize);
        Page<UserGroup> userGroupPage = userGroupService.getUserGroupPage(page, userId, parentUserId);
        return AjaxResult.success(userGroupPage);
    }
    
    /**
     * 验证是否可以添加用户组关系
     */
    @GetMapping("/validate")
    public AjaxResult validate(@RequestParam Long userId, @RequestParam Long parentUserId) {
        boolean valid = userGroupService.validateUserGroup(userId, parentUserId);
        return AjaxResult.success(valid);
    }
    
    /**
     * 快速添加用户组关系
     */
    @PostMapping("/addRelation")
    public AjaxResult addRelation(@RequestParam Long userId, @RequestParam Long parentUserId) {
        boolean result = userGroupService.addUserGroup(userId, parentUserId);
        if (result) {
            return AjaxResult.successI18n("user.group.add.success");
        } else {
            return AjaxResult.errorI18n("user.group.add.error.general");
        }
    }
} 