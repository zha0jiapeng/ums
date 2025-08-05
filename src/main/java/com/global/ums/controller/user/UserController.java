package com.global.ums.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.global.ums.annotation.RequireAuth;
import com.global.ums.entity.User;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UserService;
import com.global.ums.utils.LoginUserContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequireAuth
public class UserController {

    @Autowired
    private UserService userService;


    /**
     * 添加用户
     */
    @PostMapping("/add")
    public AjaxResult add(@RequestBody User user) {
        return userService.addUser(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        boolean result = userService.removeById(id);
        if (result) {
            return AjaxResult.successI18n("user.delete.success");
        } else {
            return AjaxResult.errorI18n("user.delete.error");
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/update")
    public AjaxResult update(@RequestBody User user) {
        boolean result = userService.updateById(user);
        if (result) {
            return AjaxResult.successI18n("user.update.success");
        } else {
            return AjaxResult.errorI18n("user.update.error");
        }
    }

    /**
     * 获取用户详情 
     */
    @GetMapping("/info")
    public AjaxResult info() {
        Long userId = LoginUserContextHolder.getUserId();
        User user = userService.getUserWithProperties(userId);
        if (user != null) {
            return AjaxResult.success(user);
        } else {
            return AjaxResult.errorI18n("user.not.found");
        }
    }

    /**
     * 分页查询用户 - 使用高压缩等级
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") Integer pageNum,
                        @RequestParam(defaultValue = "10") Integer pageSize,
                        @RequestParam(required = false) Integer type,
                        @RequestParam(required = false) String uniqueId) {
        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> userPage = userService.getUserPage(page, type, uniqueId);
        return AjaxResult.success(userPage);
    }
} 